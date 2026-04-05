package com.example.cosmos_discovery.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class EventTest {

    // ── Status constants ──────────────────────────────────────────────────

    @Test
    public void statusConstants_haveCorrectValues() {
        assertEquals("approved", Event.STATUS_APPROVED);
        assertEquals("pending",  Event.STATUS_PENDING);
        assertEquals("rejected", Event.STATUS_REJECTED);
    }

    // ── Public constructor ────────────────────────────────────────────────

    @Test
    public void constructor_setsAllFields() {
        Event e = new Event("Hackathon", 1_000L, "LUMS", null, "img.jpg", "org-1");
        assertEquals("Hackathon", e.getTitle());
        assertEquals(1_000L,      e.getDateTime());
        assertEquals("LUMS",      e.getLocation());
        assertEquals("img.jpg",   e.getImageUrl());
        assertEquals("org-1",     e.getOrganizerId());
    }

    @Test
    public void constructor_defaultStatusIsPending() {
        Event e = new Event("Hackathon", 1_000L, "LUMS", null, null, null);
        assertEquals(Event.STATUS_PENDING, e.getStatus());
    }

    @Test
    public void constructor_defaultRsvpCountIsZero() {
        Event e = new Event("Hackathon", 1_000L, "LUMS", null, null, null);
        assertEquals(0, e.getRsvpCount());
    }

    @Test
    public void constructor_createdAtIsSet() {
        long before = System.currentTimeMillis();
        Event e = new Event("Hackathon", 1_000L, "LUMS", null, null, null);
        long after = System.currentTimeMillis();
        assertTrue(e.getCreatedAt() >= before);
        assertTrue(e.getCreatedAt() <= after);
    }

    // ── Protected no-arg constructor (Firestore deserialization) ──────────

    @Test
    public void noArgConstructor_doesNotThrow() {
        Event e = new Event();
        assertNotNull(e);
    }

    @Test
    public void noArgConstructor_fieldsAreDefault() {
        Event e = new Event();
        assertNull(e.getTitle());
        assertNull(e.getLocation());
        assertNull(e.getStatus());
        assertEquals(0, e.getRsvpCount());
    }

    // ── isRsvped ──────────────────────────────────────────────────────────

    @Test
    public void isRsvped_nullAttendeeIds_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        assertFalse(e.isRsvped("uid-1"));
    }

    @Test
    public void isRsvped_uidInList_returnsTrue() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setAttendeeIds(Arrays.asList("uid-1", "uid-2"));
        assertTrue(e.isRsvped("uid-1"));
    }

    @Test
    public void isRsvped_uidNotInList_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setAttendeeIds(Collections.singletonList("uid-2"));
        assertFalse(e.isRsvped("uid-1"));
    }

    @Test
    public void isRsvped_emptyAttendeeList_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setAttendeeIds(Collections.emptyList());
        assertFalse(e.isRsvped("uid-1"));
    }

    // ── Firestore field contract ──────────────────────────────────────────

    @Test
    public void firestoreFields_setAndGetRoundTrip() {
        Event e = new Event();
        e.setId("doc-id");
        e.setTitle("Seminar");
        e.setDateTime(2_000L);
        e.setLocation("Auditorium");
        e.setTags(Arrays.asList("tech", "free"));
        e.setRsvpCount(5);
        e.setImageUrl("https://img.png");
        e.setOrganizerId("org-99");
        e.setStatus(Event.STATUS_APPROVED);
        e.setAttendeeIds(Arrays.asList("u1", "u2"));
        e.setCreatedAt(1_000L);
        e.setAccessType("lums_only");

        assertEquals("doc-id",            e.getId());
        assertEquals("Seminar",           e.getTitle());
        assertEquals(2_000L,              e.getDateTime());
        assertEquals("Auditorium",        e.getLocation());
        assertEquals(Arrays.asList("tech", "free"), e.getTags());
        assertEquals(5,                   e.getRsvpCount());
        assertEquals("https://img.png",   e.getImageUrl());
        assertEquals("org-99",            e.getOrganizerId());
        assertEquals(Event.STATUS_APPROVED, e.getStatus());
        assertEquals(Arrays.asList("u1", "u2"), e.getAttendeeIds());
        assertEquals(1_000L,              e.getCreatedAt());
        assertEquals("lums_only",         e.getAccessType());
    }
}
