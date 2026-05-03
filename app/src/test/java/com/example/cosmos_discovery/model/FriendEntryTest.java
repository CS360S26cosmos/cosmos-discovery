package com.example.cosmos_discovery.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class FriendEntryTest {

    // ── Public constructor ────────────────────────────────────────────────

    @Test
    public void constructor_setsUidNamePhotoUrl() {
        FriendEntry entry = new FriendEntry("uid-1", "Sara", "https://photo.jpg");
        assertEquals("uid-1",             entry.getUid());
        assertEquals("Sara",              entry.getName());
        assertEquals("https://photo.jpg", entry.getPhotoUrl());
    }

    @Test
    public void constructor_stampsAddedAt() {
        long before = System.currentTimeMillis();
        FriendEntry entry = new FriendEntry("uid-1", "Sara", null);
        long after = System.currentTimeMillis();
        assertTrue(entry.getAddedAt() >= before);
        assertTrue(entry.getAddedAt() <= after);
    }

    @Test
    public void constructor_nullPhotoUrl_allowed() {
        FriendEntry entry = new FriendEntry("uid-1", "Sara", null);
        assertNull(entry.getPhotoUrl());
    }

    // ── Protected no-arg constructor (Firestore deserialization) ──────────

    @Test
    public void noArgConstructor_doesNotThrow() {
        FriendEntry entry = new FriendEntry();
        assertNotNull(entry);
    }

    @Test
    public void noArgConstructor_fieldsAreNull() {
        FriendEntry entry = new FriendEntry();
        assertNull(entry.getUid());
        assertNull(entry.getName());
        assertNull(entry.getPhotoUrl());
        assertEquals(0L, entry.getAddedAt());
    }

    // ── Firestore field contract ──────────────────────────────────────────

    @Test
    public void firestoreFields_setAndGetRoundTrip() {
        FriendEntry entry = new FriendEntry();
        entry.setUid("uid-42");
        entry.setName("Ahmed");
        entry.setPhotoUrl("https://cdn.img/ahmed.jpg");
        entry.setAddedAt(5_000L);

        assertEquals("uid-42",                   entry.getUid());
        assertEquals("Ahmed",                    entry.getName());
        assertEquals("https://cdn.img/ahmed.jpg", entry.getPhotoUrl());
        assertEquals(5_000L,                     entry.getAddedAt());
    }
}
