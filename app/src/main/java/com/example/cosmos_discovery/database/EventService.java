package com.example.cosmos_discovery.database;

import android.util.Log;

import com.example.cosmos_discovery.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Provides event-related Firestore operations.
 *
 * All methods are async. Use {@code Consumer<T> onSuccess} for results and
 * {@code Consumer<String> onFailure} for error messages — the same pattern as
 * {@link AuthService}.
 */
public class EventService {

    private static final String TAG               = "EventService";
    private static final String EVENTS_COLLECTION = "events"; // Firestore collection name

    private final FirebaseFirestore mDb;

    /** Creates a new EventService backed by the default FirebaseFirestore instance. */
    public EventService() {
        mDb = FirebaseFirestore.getInstance();
    }

    // ── Fetch ─────────────────────────────────────────────────────────────

    /**
     * Attaches a real-time listener for approved events in the current calendar week
     * (Sunday 00:00 → Saturday 23:59:59). {@code onUpdate} fires immediately with the
     * current data, then again whenever a document is added, changed, or removed.
     *
     * NOTE: Requires a composite Firestore index on (status ASC, dateTime ASC).
     * On first run, Logcat will print a link — click it to auto-create the index.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}
     *         to detach and stop streaming updates.
     */
    public ListenerRegistration listenThisWeekEvents(Consumer<List<Event>> onUpdate,
                                                     Consumer<String> onError) {
        long[] weekBounds = currentWeekBounds();
        return mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .whereGreaterThanOrEqualTo("dateTime", weekBounds[0])
                .whereLessThanOrEqualTo("dateTime", weekBounds[1])
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load events.");
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Attaches a real-time listener for all approved events with a future dateTime,
     * ordered chronologically. Use this for a full "upcoming events" feed.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}
     */
    public ListenerRegistration listenUpcomingEvents(Consumer<List<Event>> onUpdate,
                                                     Consumer<String> onError) {
        long now = System.currentTimeMillis();
        return mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .whereGreaterThanOrEqualTo("dateTime", now)
                .orderBy("dateTime")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load events.");
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Attaches a real-time listener for all events where {@code userId} is in the
     * {@code attendeeIds} array (i.e. the user has RSVPed). Returns the full list
     * unsorted — callers should split and sort client-side into upcoming / past.
     *
     * Uses a single {@code array-contains} filter, which requires no composite index.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}
     */
    public ListenerRegistration listenMyRsvpedEvents(String userId,
                                                      Consumer<List<Event>> onUpdate,
                                                      Consumer<String> onError) {
        return mDb.collection(EVENTS_COLLECTION)
                .whereArrayContains("attendeeIds", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load your events.");
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Attaches a real-time listener for all events created by the given organizer.
     * No server-side ordering — callers sort client-side after splitting by status.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}.
     */
    public ListenerRegistration listenOrganizerEvents(String organizerId,
                                                      Consumer<List<Event>> onUpdate,
                                                      Consumer<String> onError) {
        Log.d("EventService", "listenOrganizerEvents: organizerId=" + organizerId);
        return mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("EventService", "listenOrganizerEvents ERROR: " + error.getMessage(), error);
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load your events.");
                        return;
                    }
                    Log.d("EventService", "listenOrganizerEvents: snapshot has "
                            + (snapshot != null ? snapshot.size() : "null") + " docs");
                    List<Event> events = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Event e = doc.toObject(Event.class);
                            if (e != null) {
                                e.setId(doc.getId());
                                events.add(e);
                            }
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Fetches the {@code limit} most recently created approved events,
     * ordered by {@code createdAt} descending. Used for "Suggested" cards.
     */
    public void fetchRecentEvents(int limit,
                                  Consumer<List<Event>> onSuccess,
                                  Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onSuccess.accept(events);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Failed to load events."));
    }

    // ── Create / Delete / Update ──────────────────────────────────────────

    /**
     * Writes a new event document to Firestore. The document ID is auto-generated.
     * {@code onSuccess} receives the new document ID so callers can set {@code event.setId()}.
     */
    public void addEvent(Event event,
                         Consumer<String> onSuccess,
                         Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .add(event)
                .addOnSuccessListener(ref -> onSuccess.accept(ref.getId()))
                .addOnFailureListener(ex -> onFailure.accept("Could not create event. Try again."));
    }

    /**
     * Deletes the event document with the given ID.
     */
    public void removeEvent(String eventId,
                            Runnable onSuccess,
                            Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not delete event. Try again."));
    }

    /**
     * Updates the {@code status} field of an event. Use the constants on {@link Event}:
     * {@code Event.STATUS_APPROVED}, {@code Event.STATUS_PENDING}, {@code Event.STATUS_REJECTED}.
     */
    public void updateEventStatus(String eventId, String status,
                                  Runnable onSuccess,
                                  Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("status", status)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not update event status."));
    }

    /**
     * Updates arbitrary fields on an event document.
     */
    public void updateEvent(String eventId,
                            Map<String, Object> fields,
                            Runnable onSuccess,
                            Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(fields)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not update event."));
    }

    /**
     * One-shot fetch of all approved upcoming events ordered chronologically.
     * Use this for polling — call on a fixed interval instead of keeping a listener open.
     */
    public void fetchUpcomingEvents(Consumer<List<Event>> onSuccess,
                                    Consumer<String> onFailure) {
        long now = System.currentTimeMillis();
        mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .whereGreaterThanOrEqualTo("dateTime", now)
                .orderBy("dateTime")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onSuccess.accept(events);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Failed to load events."));
    }

    /**
     * Fetches a single event by its document ID. One-shot read.
     */
    public void fetchEventById(String eventId,
                               Consumer<Event> onSuccess,
                               Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Event e = doc.toObject(Event.class);
                    if (e != null) {
                        e.setId(doc.getId());
                        onSuccess.accept(e);
                    } else {
                        onFailure.accept("Event not found.");
                    }
                })
                .addOnFailureListener(ex -> onFailure.accept("Could not fetch event."));
    }

    // ── RSVP ──────────────────────────────────────────────────────────────

    /**
     * Adds {@code userId} to the event's {@code attendeeIds} array and increments
     * {@code rsvpCount} by 1. Both updates are applied atomically via a single
     * Firestore document update.
     */
    public void rsvpToEvent(String eventId, String userId,
                            Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(
                        "attendeeIds", FieldValue.arrayUnion(userId),
                        "rsvpCount",   FieldValue.increment(1)
                )
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not RSVP. Try again."));
    }

    /**
     * Removes {@code userId} from the event's {@code attendeeIds} array and decrements
     * {@code rsvpCount} by 1. Both updates are applied atomically.
     */
    public void cancelRsvp(String eventId, String userId,
                           Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(
                        "attendeeIds", FieldValue.arrayRemove(userId),
                        "rsvpCount",   FieldValue.increment(-1)
                )
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not cancel RSVP. Try again."));
    }

    // ── Categories ────────────────────────────────────────────────────────

    /**
     * One-shot fetch of all category names from the {@code categories} collection.
     * Each document must have a {@code name} String field. Results are sorted alphabetically.
     * Used to populate the filter overlay's category chips.
     */
    public void fetchCategories(Consumer<List<String>> onSuccess, Consumer<String> onFailure) {
        mDb.collection("categories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) names.add(name);
                    }
                    Collections.sort(names);
                    onSuccess.accept(names);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Failed to load categories."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Returns [weekStartMs, weekEndMs] for the current Sun–Sat week. */
    private long[] currentWeekBounds() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_WEEK, 6);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }
}
