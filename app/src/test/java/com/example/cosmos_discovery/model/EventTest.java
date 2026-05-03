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

    // ── hasCapacity / isFull ──────────────────────────────────────────────

    @Test
    public void hasCapacity_zeroCapacity_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(0);
        assertFalse(e.hasCapacity());
    }

    @Test
    public void hasCapacity_positiveCapacity_returnsTrue() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(50);
        assertTrue(e.hasCapacity());
    }

    @Test
    public void isFull_unlimitedCapacity_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(0);
        e.setRsvpCount(9999);
        assertFalse(e.isFull());
    }

    @Test
    public void isFull_belowCapacity_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(100);
        e.setRsvpCount(99);
        assertFalse(e.isFull());
    }

    @Test
    public void isFull_atCapacity_returnsTrue() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(100);
        e.setRsvpCount(100);
        assertTrue(e.isFull());
    }

    @Test
    public void isFull_overCapacity_returnsTrue() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(100);
        e.setRsvpCount(101);
        assertTrue(e.isFull());
    }

    // ── isRegistrationClosed ──────────────────────────────────────────────

    @Test
    public void isRegistrationClosed_nullRegisterBy_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        assertFalse(e.isRegistrationClosed());
    }

    @Test
    public void isRegistrationClosed_emptyRegisterBy_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setRegisterBy("   ");
        assertFalse(e.isRegistrationClosed());
    }

    @Test
    public void isRegistrationClosed_unparseableRegisterBy_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setRegisterBy("not a date");
        assertFalse(e.isRegistrationClosed());
    }

    @Test
    public void isRegistrationClosed_pastDeadline_returnsTrue() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setRegisterBy("2000-01-01");
        assertTrue(e.isRegistrationClosed());
    }

    @Test
    public void isRegistrationClosed_futureDeadline_returnsFalse() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setRegisterBy("2099-12-31");
        assertFalse(e.isRegistrationClosed());
    }

    // ── getSpotsText ──────────────────────────────────────────────────────

    @Test
    public void getSpotsText_unlimitedCapacity_showsRsvpCount() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(0);
        e.setRsvpCount(23);
        assertEquals("23 RSVPs", e.getSpotsText());
    }

    @Test
    public void getSpotsText_withCapacity_showsRatio() {
        Event e = new Event("E", 0L, "L", null, null, null);
        e.setCapacity(100);
        e.setRsvpCount(23);
        assertEquals("23/100 spots taken", e.getSpotsText());
    }

    @Test
    public void getSpotsText_zeroRsvps_unlimitedCapacity() {
        Event e = new Event("E", 0L, "L", null, null, null);
        assertEquals("0 RSVPs", e.getSpotsText());
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
        e.setDescription("desc");
        e.setCategory("cat");
        e.setDateOfEvent("2026-04-06");
        e.setStartTime("5:00pm");
        e.setEndTime("7:00pm");
        e.setRegisterBy("2026-04-01");

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
        assertEquals("desc",              e.getDescription());
        assertEquals("cat",               e.getCategory());
        assertEquals("2026-04-06",        e.getDateOfEvent());
        assertEquals("5:00pm",            e.getStartTime());
        assertEquals("7:00pm",            e.getEndTime());
        assertEquals("2026-04-01",        e.getRegisterBy());
    }
}
