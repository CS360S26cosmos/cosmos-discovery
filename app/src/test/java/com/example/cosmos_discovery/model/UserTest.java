package com.example.cosmos_discovery.model;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.cosmos_discovery.model.User;

public class UserTest {

    // ── Empty constructor ─────────────────────────────────────────────────
    // MOST IMPORTANT TEST: Firestore requires this to deserialize documents.
    // If this fails, snapshot.toObject(User.class) will always return null.

    @Test
    public void emptyConstructor_doesNotThrow() {
        User user = new User();
        assertNotNull(user);
    }

    @Test
    public void emptyConstructor_fieldsAreNull() {
        User user = new User();
        assertNull(user.getUid());
        assertNull(user.getEmail());
        assertNull(user.getName());
        assertNull(user.getRole());
    }

    // ── Parameterised constructor ─────────────────────────────────────────

    @Test
    public void paramConstructor_setsUidEmailName() {
        User user = new User("uid123", "student@lums.edu.pk", "Ali Ahmed");
        assertEquals("uid123", user.getUid());
        assertEquals("student@lums.edu.pk", user.getEmail());
        assertEquals("Ali Ahmed", user.getName());
    }

    @Test
    public void paramConstructor_defaultRoleIsStudent() {
        User user = new User("uid123", "student@lums.edu.pk", "Ali Ahmed");
        assertEquals("student", user.getRole());
    }

    @Test
    public void paramConstructor_defaultIsActiveTrue() {
        User user = new User("uid123", "student@lums.edu.pk", "Ali Ahmed");
        assertTrue(user.isActive());
    }

    @Test
    public void paramConstructor_createdAtIsSet() {
        long before = System.currentTimeMillis();
        User user = new User("uid123", "student@lums.edu.pk", "Ali Ahmed");
        long after = System.currentTimeMillis();
        assertTrue(user.getCreatedAt() >= before);
        assertTrue(user.getCreatedAt() <= after);
    }

    // ── Role constants ────────────────────────────────────────────────────

    @Test
    public void roleConstants_haveCorrectValues() {
        assertEquals("student",   User.ROLE_STUDENT);
        assertEquals("organizer", User.ROLE_ORGANIZER);
        assertEquals("admin",     User.ROLE_ADMIN);
    }

    // ── Setters ───────────────────────────────────────────────────────────

    @Test
    public void setRole_updatesRole() {
        User user = new User("uid123", "student@lums.edu.pk", "Ali");
        user.setRole(User.ROLE_ORGANIZER);
        assertEquals("organizer", user.getRole());
    }

    @Test
    public void setActive_false_updatesFlag() {
        User user = new User("uid123", "student@lums.edu.pk", "Ali");
        user.setActive(false);
        assertFalse(user.isActive());
    }

    @Test
    public void setBatch_andGet_works() {
        User user = new User();
        user.setBatch("2027");
        assertEquals("2027", user.getBatch());
    }

    @Test
    public void setMajor_andGet_works() {
        User user = new User();
        user.setMajor("Computer Science");
        assertEquals("Computer Science", user.getMajor());
    }



    // ── Firestore field name check (documentation test) ───────────────────
    // This test documents which field names Firestore will use.
    // Firestore maps Java field names directly. If you rename a field,
    // existing Firestore documents will break. This test catches renames.

    @Test
    public void firestoreFields_setAndGetRoundTrip() {
        User user = new User();
        user.setUid("u1");
        user.setEmail("a@lums.edu.pk");
        user.setName("Test User");
        user.setRole("admin");
        user.setActive(true);
        user.setBatch("2026");
        user.setMajor("EE");
        user.setBio("Hello");
        user.setCreatedAt(1000L);

        assertEquals("u1",            user.getUid());
        assertEquals("a@lums.edu.pk", user.getEmail());
        assertEquals("Test User",     user.getName());
        assertEquals("admin",         user.getRole());
        assertTrue(                   user.isActive());
        assertEquals("2026",          user.getBatch());
        assertEquals("EE",            user.getMajor());
        assertEquals("Hello",         user.getBio());
        assertEquals(1000L,           user.getCreatedAt());
    }
}