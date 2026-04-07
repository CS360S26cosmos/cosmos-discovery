package com.example.cosmos_discovery.database;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cosmos_discovery.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Integration tests for EventService — hits real Firestore.
 *
 * HOW TO RUN:
 * - Connect a physical device (USB Debugging on) or start an emulator
 * - Right-click this file in Android Studio → Run 'EventServiceIntegrationTest'
 * - Watch Firebase Console → Firestore → events collection to see documents
 *   appear and disappear in real time
 *
 * NOTE: Not run in CI. CI only runs src/test/ unit tests via ./gradlew test.
 */
@RunWith(AndroidJUnit4.class)
public class EventServiceIntegrationTest {

    private static final String IMAGE_URL    =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSKWmPsj1eNi4KLT5gVaTIHCxbRZbiJFHtaFA&s";
    private static final String TEST_EMAIL    = "27100026@lums.edu.pk";
    private static final String TEST_PASSWORD = "Test@321";
    private static final int    TIMEOUT_SECS  = 15;

    private EventService      mService;
    private FirebaseFirestore mDb;

    /** Tracks every event ID created during tests so @After can clean them up. */
    private final List<String> mCreatedIds = new ArrayList<>();

    // ── Setup & Teardown ──────────────────────────────────────────────────

    @Before
    public void setUp() throws InterruptedException {
        mService = new EventService();
        mDb      = FirebaseFirestore.getInstance();

        // Sign in if not already signed in (previous test class may have signed out).
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            CountDownLatch latch = new CountDownLatch(1);
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
                    .addOnCompleteListener(task -> latch.countDown());
            assertTrue("Sign-in timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        }
        assertNotNull("Sign-in failed — check TEST_EMAIL/TEST_PASSWORD credentials",
                FirebaseAuth.getInstance().getCurrentUser());
    }

    @After
    public void tearDown() throws InterruptedException {
        // Delete only the events that were created during this test.
        // mCreatedIds is populated solely by addEventAndTrack(), so pre-existing
        // events in Firestore are never tracked here and will never be touched.
        for (String id : new ArrayList<>(mCreatedIds)) {
            CountDownLatch latch = new CountDownLatch(1);
            mDb.collection("events").document(id)
                    .delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(TIMEOUT_SECS, TimeUnit.SECONDS);
        }
        mCreatedIds.clear();
    }

    // ── Test 1: Add a single event ────────────────────────────────────────

    /**
     * Adds one event and immediately fetches it back by ID.
     * Verifies every field round-trips correctly through Firestore.
     */
    @Test
    public void addEvent_singleEvent_allFieldsRoundTripCorrectly()
            throws InterruptedException {

        Event event = new Event(
                "Campus Comedy Jam",
                System.currentTimeMillis() + 86_400_000L,
                "Sports Complex",
                Arrays.asList("Music", "Party", "Campus"),
                IMAGE_URL,
                "test-organizer"
        );
        event.setStatus(Event.STATUS_APPROVED);

        // Add
        String id = addEventAndTrack(event);
        assertNotNull("addEvent returned null ID", id);

        // Fetch back and verify
        Event fetched = fetchById(id);
        assertNotNull("fetchEventById returned null", fetched);
        assertEquals("title mismatch",     "Campus Comedy Jam", fetched.getTitle());
        assertEquals("location mismatch",  "Sports Complex",    fetched.getLocation());
        assertEquals("imageUrl mismatch",  IMAGE_URL,           fetched.getImageUrl());
        assertEquals("status mismatch",    Event.STATUS_APPROVED, fetched.getStatus());
        assertEquals("tags size mismatch", 3, fetched.getTags().size());
        assertTrue("tags missing Music",   fetched.getTags().contains("Music"));
    }

    // ── Test 2: Add seven events ──────────────────────────────────────────

    /**
     * Adds 7 slightly different events in sequence.
     * Verifies each one is independently retrievable by its own document ID.
     */
    @Test
    public void addEvent_sevenEvents_allRetrievableByTheirId()
            throws InterruptedException {

        long base = System.currentTimeMillis();

        Event[] events = {
            makeEvent("Campus Comedy Jam",          base + 1 * 86_400_000L, "Sports Complex",      Arrays.asList("Music",    "Party",    "Campus")),
            makeEvent("AI Research Symposium",      base + 2 * 86_400_000L, "SBASSE Auditorium",   Arrays.asList("Academic", "Tech")),
            makeEvent("LUMS Food Festival",         base + 3 * 86_400_000L, "PDC Courtyard",       Arrays.asList("Food",     "Social",   "Campus")),
            makeEvent("Photography Workshop",       base + 4 * 86_400_000L, "SSE Lawn",            Arrays.asList("Art",      "Workshop")),
            makeEvent("Cricket Tournament",         base + 5 * 86_400_000L, "Sports Ground",       Arrays.asList("Sports",   "Campus")),
            makeEvent("Startup Pitch Night",        base + 6 * 86_400_000L, "LUMS Auditorium",     Arrays.asList("Tech",     "Business")),
            makeEvent("Mental Health Talk",         base + 7 * 86_400_000L, "Cafe 98",             Arrays.asList("Health",   "Social"))
        };

        String[] ids = new String[7];
        for (int i = 0; i < events.length; i++) {
            ids[i] = addEventAndTrack(events[i]);
            assertNotNull("addEvent returned null ID for event " + i, ids[i]);
        }

        // Verify each event is retrievable and has the right title
        for (int i = 0; i < ids.length; i++) {
            Event fetched = fetchById(ids[i]);
            assertNotNull("Could not fetch event " + i, fetched);
            assertEquals("title mismatch for event " + i,
                    events[i].getTitle(), fetched.getTitle());
            assertEquals("imageUrl mismatch for event " + i,
                    IMAGE_URL, fetched.getImageUrl());
        }
    }

    // ── Test 3: Remove an event ───────────────────────────────────────────

    /**
     * Adds an event, removes it, then tries to fetch it.
     * Verifies the document no longer exists after deletion.
     */
    @Test
    public void removeEvent_existingEvent_disappearsFromFirestore()
            throws InterruptedException {

        String id = addEventAndTrack(makeEvent("Event To Delete",
                System.currentTimeMillis() + 86_400_000L, "Test Hall",
                Arrays.asList("Test")));
        assertNotNull(id);

        // Remove
        CountDownLatch removeLatch  = new CountDownLatch(1);
        AtomicReference<String> err = new AtomicReference<>();
        mService.removeEvent(id,
                () -> removeLatch.countDown(),
                e  -> { err.set(e); removeLatch.countDown(); });
        assertTrue("removeEvent timed out", removeLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("removeEvent failed: " + err.get(), err.get());
        mCreatedIds.remove(id); // already deleted — don't attempt again in @After

        // Verify gone — Firestore returns an empty snapshot (not an error) for missing docs
        CountDownLatch fetchLatch = new CountDownLatch(1);
        AtomicReference<Boolean> exists = new AtomicReference<>(false);
        mDb.collection("events").document(id)
                .get()
                .addOnSuccessListener(doc -> { exists.set(doc.exists()); fetchLatch.countDown(); })
                .addOnFailureListener(e  -> fetchLatch.countDown());
        assertTrue("Post-delete fetch timed out", fetchLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertFalse("Event document still exists after removeEvent", exists.get());
    }

    // ── Test 4: RSVP to an event ──────────────────────────────────────────

    /**
     * Adds an event, RSVPs with a test user ID, then fetches the document.
     * Verifies attendeeIds contains the user and rsvpCount incremented to 1.
     */
    @Test
    public void rsvpToEvent_addsUserAndIncrementsCount()
            throws InterruptedException {

        String id = addEventAndTrack(makeEvent("RSVP Test Event",
                System.currentTimeMillis() + 86_400_000L, "RSVP Hall",
                Arrays.asList("Test")));

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> err = new AtomicReference<>();
        mService.rsvpToEvent(id, uid,
                () -> latch.countDown(),
                e  -> { err.set(e); latch.countDown(); });
        assertTrue("rsvpToEvent timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("rsvpToEvent failed: " + err.get(), err.get());

        Event updated = fetchById(id);
        assertNotNull(updated);
        assertEquals("rsvpCount should be 1", 1, updated.getRsvpCount());
        assertNotNull("attendeeIds should not be null", updated.getAttendeeIds());
        assertTrue("attendeeIds should contain uid", updated.getAttendeeIds().contains(uid));
    }

    // ── Test 5: Cancel RSVP ───────────────────────────────────────────────

    /**
     * Adds an event, RSVPs, then cancels. Verifies attendeeIds is empty
     * and rsvpCount is back to 0.
     */
    @Test
    public void cancelRsvp_removesUserAndDecrementsCount()
            throws InterruptedException {

        String id = addEventAndTrack(makeEvent("Cancel RSVP Test",
                System.currentTimeMillis() + 86_400_000L, "Cancel Hall",
                Arrays.asList("Test")));

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // RSVP first
        CountDownLatch rsvpLatch = new CountDownLatch(1);
        mService.rsvpToEvent(id, uid, rsvpLatch::countDown, e -> rsvpLatch.countDown());
        assertTrue(rsvpLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));

        // Now cancel
        CountDownLatch cancelLatch = new CountDownLatch(1);
        AtomicReference<String> err = new AtomicReference<>();
        mService.cancelRsvp(id, uid,
                () -> cancelLatch.countDown(),
                e  -> { err.set(e); cancelLatch.countDown(); });
        assertTrue("cancelRsvp timed out", cancelLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("cancelRsvp failed: " + err.get(), err.get());

        Event updated = fetchById(id);
        assertNotNull(updated);
        assertEquals("rsvpCount should be back to 0", 0, updated.getRsvpCount());
        assertFalse("attendeeIds should not contain uid",
                updated.getAttendeeIds() != null && updated.getAttendeeIds().contains(uid));
    }

    // ── Test 6: Update event status ───────────────────────────────────────

    /**
     * Adds a pending event, approves it via updateEventStatus, then fetches.
     * Verifies status changed from pending to approved.
     */
    @Test
    public void updateEventStatus_pendingToApproved_statusChanges()
            throws InterruptedException {

        Event event = makeEvent("Pending Event",
                System.currentTimeMillis() + 86_400_000L, "Pending Hall",
                Arrays.asList("Test"));
        // Default status from constructor is STATUS_PENDING — no need to set it
        String id = addEventAndTrack(event);

        Event before = fetchById(id);
        assertEquals("should start as pending", Event.STATUS_PENDING, before.getStatus());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> err = new AtomicReference<>();
        mService.updateEventStatus(id, Event.STATUS_APPROVED,
                () -> latch.countDown(),
                e  -> { err.set(e); latch.countDown(); });
        assertTrue("updateEventStatus timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("updateEventStatus failed: " + err.get(), err.get());

        Event after = fetchById(id);
        assertEquals("status should now be approved", Event.STATUS_APPROVED, after.getStatus());
    }

    // ── Test 7: fetchEventById ────────────────────────────────────────────

    /**
     * Adds an event, fetches it by ID via EventService (not direct Firestore).
     * Verifies the ID is correctly set on the returned object.
     */
    @Test
    public void fetchEventById_returnsEventWithCorrectId()
            throws InterruptedException {

        String id = addEventAndTrack(makeEvent("Fetch By ID Test",
                System.currentTimeMillis() + 86_400_000L, "Fetch Hall",
                Arrays.asList("Test")));

        CountDownLatch latch              = new CountDownLatch(1);
        AtomicReference<Event> result     = new AtomicReference<>();
        AtomicReference<String> err       = new AtomicReference<>();
        mService.fetchEventById(id,
                e  -> { result.set(e); latch.countDown(); },
                msg -> { err.set(msg); latch.countDown(); });
        assertTrue("fetchEventById timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("fetchEventById failed: " + err.get(), err.get());
        assertNotNull(result.get());
        assertEquals("returned event ID should match", id, result.get().getId());
    }

    // ── Test 8: fetchEventById — non-existent ID ──────────────────────────

    /**
     * Calls fetchEventById with a document ID that doesn't exist.
     * Verifies the error callback fires rather than returning null silently.
     */
    @Test
    public void fetchEventById_nonExistentId_callsErrorCallback()
            throws InterruptedException {

        CountDownLatch latch           = new CountDownLatch(1);
        AtomicReference<String> error  = new AtomicReference<>();
        mService.fetchEventById("this-id-does-not-exist-xyz",
                e   -> latch.countDown(),    // should NOT be called
                msg -> { error.set(msg); latch.countDown(); });
        assertTrue("fetchEventById timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNotNull("Expected error for non-existent ID but got none", error.get());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Creates an approved Event with the shared test image URL. */
    private Event makeEvent(String title, long dateTime, String location, List<String> tags) {
        Event e = new Event(title, dateTime, location, tags, IMAGE_URL, "test-organizer");
        e.setStatus(Event.STATUS_APPROVED);
        return e;
    }

    /**
     * Calls {@link EventService#addEvent}, blocks until complete, tracks the ID
     * for cleanup in {@link #tearDown()}, and returns the ID (or null on failure).
     */
    private String addEventAndTrack(Event event) throws InterruptedException {
        CountDownLatch latch             = new CountDownLatch(1);
        AtomicReference<String> idRef    = new AtomicReference<>();
        mService.addEvent(event,
                id  -> { idRef.set(id); latch.countDown(); },
                err -> latch.countDown());
        assertTrue("addEvent timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        String id = idRef.get();
        if (id != null) mCreatedIds.add(id);
        return id;
    }

    /** Fetches an event by ID via direct Firestore and returns the Event, or null. */
    private Event fetchById(String id) throws InterruptedException {
        CountDownLatch latch            = new CountDownLatch(1);
        AtomicReference<Event> result   = new AtomicReference<>();
        mService.fetchEventById(id,
                e   -> { result.set(e); latch.countDown(); },
                err -> latch.countDown());
        assertTrue("fetchById timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        return result.get();
    }
}
