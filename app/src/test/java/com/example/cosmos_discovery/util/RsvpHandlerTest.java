package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Tests RsvpHandler's optimistic local state changes.
 *
 * Events with a null ID skip the Firestore write but still apply the local update
 * and call onRefresh — this lets us test all state logic without Firebase.
 */
public class RsvpHandlerTest {

    private static final String TEST_UID = "test-uid-123";
    private RsvpHandler mHandler;

    @Before
    public void setUp() {
        User user = new User(TEST_UID, "test@lums.edu.pk", "Test User");
        RoleUtil.setCurrentUser(user);
        mHandler = new RsvpHandler();
    }

    @After
    public void tearDown() {
        RoleUtil.clear();
    }

    private Event notRsvpedEvent() {
        // null id skips Firestore, applies optimistic update immediately
        Event e = new Event("title", 0L, "loc", null, null, null);
        e.setAttendeeIds(new ArrayList<>());
        e.setRsvpCount(0);
        return e;
    }

    private Event rsvpedEvent() {
        Event e = new Event("title", 0L, "loc", null, null, null);
        e.setAttendeeIds(new ArrayList<>(Collections.singletonList(TEST_UID)));
        e.setRsvpCount(1);
        return e;
    }

    // ── Add RSVP ────────────────────────────────────────────────────────────

    @Test
    public void toggle_notRsvped_addsUidToAttendeeIds() {
        Event event = notRsvpedEvent();
        mHandler.toggle(event, () -> {}, err -> {});
        assertTrue(event.getAttendeeIds().contains(TEST_UID));
    }

    @Test
    public void toggle_notRsvped_incrementsRsvpCount() {
        Event event = notRsvpedEvent();
        mHandler.toggle(event, () -> {}, err -> {});
        assertEquals(1, event.getRsvpCount());
    }

    @Test
    public void toggle_notRsvped_callsOnRefresh() {
        boolean[] called = {false};
        mHandler.toggle(notRsvpedEvent(), () -> called[0] = true, err -> {});
        assertTrue(called[0]);
    }

    // ── Cancel RSVP ─────────────────────────────────────────────────────────

    @Test
    public void toggle_alreadyRsvped_removesUidFromAttendeeIds() {
        Event event = rsvpedEvent();
        mHandler.toggle(event, () -> {}, err -> {});
        assertFalse(event.getAttendeeIds().contains(TEST_UID));
    }

    @Test
    public void toggle_alreadyRsvped_decrementsRsvpCount() {
        Event event = rsvpedEvent();
        mHandler.toggle(event, () -> {}, err -> {});
        assertEquals(0, event.getRsvpCount());
    }

    @Test
    public void toggle_alreadyRsvped_callsOnRefresh() {
        boolean[] called = {false};
        mHandler.toggle(rsvpedEvent(), () -> called[0] = true, err -> {});
        assertTrue(called[0]);
    }

    @Test
    public void toggle_cancelRsvp_rsvpCountNeverGoesNegative() {
        Event event = new Event("title", 0L, "loc", null, null, null);
        event.setAttendeeIds(new ArrayList<>(Collections.singletonList(TEST_UID)));
        event.setRsvpCount(0); // already 0 — cancel should clamp at 0
        mHandler.toggle(event, () -> {}, err -> {});
        assertEquals(0, event.getRsvpCount());
    }

    // ── Not logged in ────────────────────────────────────────────────────────

    @Test
    public void toggle_notLoggedIn_callsOnError() {
        RoleUtil.clear();
        boolean[] errorCalled = {false};
        mHandler.toggle(notRsvpedEvent(), () -> {}, err -> errorCalled[0] = true);
        assertTrue(errorCalled[0]);
    }

    @Test
    public void toggle_notLoggedIn_doesNotModifyEvent() {
        RoleUtil.clear();
        Event event = notRsvpedEvent();
        mHandler.toggle(event, () -> {}, err -> {});
        assertTrue(event.getAttendeeIds().isEmpty());
        assertEquals(0, event.getRsvpCount());
    }
}
