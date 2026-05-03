package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.User;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RoleUtil}, the in-memory current-user singleton.
 *
 * Covers role-check predicates, null-safety when no user is set, and the
 * {@link RoleUtil#clear()} contract used on sign-out.
 */
public class RoleUtilTest {

    @After
    public void tearDown() {
        RoleUtil.clear();
    }

    private static User userWithRole(String role) {
        User u = new User();
        u.setRole(role);
        return u;
    }

    // ── No current user ───────────────────────────────────────────────────

    @Test
    public void getCurrentUser_whenNotSet_returnsNull() {
        assertNull(RoleUtil.getCurrentUser());
    }

    @Test
    public void roleChecks_whenNoUser_allReturnFalse() {
        assertFalse(RoleUtil.isAdmin());
        assertFalse(RoleUtil.isOrganizer());
        assertFalse(RoleUtil.isStudent());
    }

    // ── Role predicates ───────────────────────────────────────────────────

    @Test
    public void isStudent_whenStudentRole_returnsTrue() {
        RoleUtil.setCurrentUser(userWithRole(User.ROLE_STUDENT));
        assertTrue(RoleUtil.isStudent());
        assertFalse(RoleUtil.isOrganizer());
        assertFalse(RoleUtil.isAdmin());
    }

    @Test
    public void isOrganizer_whenOrganizerRole_returnsTrue() {
        RoleUtil.setCurrentUser(userWithRole(User.ROLE_ORGANIZER));
        assertTrue(RoleUtil.isOrganizer());
        assertFalse(RoleUtil.isStudent());
        assertFalse(RoleUtil.isAdmin());
    }

    @Test
    public void isAdmin_whenAdminRole_returnsTrue() {
        RoleUtil.setCurrentUser(userWithRole(User.ROLE_ADMIN));
        assertTrue(RoleUtil.isAdmin());
        assertFalse(RoleUtil.isStudent());
        assertFalse(RoleUtil.isOrganizer());
    }

    @Test
    public void roleChecks_whenRoleIsNull_returnFalse() {
        RoleUtil.setCurrentUser(userWithRole(null));
        assertFalse(RoleUtil.isAdmin());
        assertFalse(RoleUtil.isOrganizer());
        assertFalse(RoleUtil.isStudent());
    }

    @Test
    public void roleChecks_whenUnknownRole_returnFalse() {
        RoleUtil.setCurrentUser(userWithRole("superuser"));
        assertFalse(RoleUtil.isAdmin());
        assertFalse(RoleUtil.isOrganizer());
        assertFalse(RoleUtil.isStudent());
    }

    // ── clear() contract ──────────────────────────────────────────────────

    @Test
    public void clear_resetsCurrentUserToNull() {
        RoleUtil.setCurrentUser(userWithRole(User.ROLE_STUDENT));
        assertNotNull(RoleUtil.getCurrentUser());

        RoleUtil.clear();

        assertNull(RoleUtil.getCurrentUser());
        assertFalse(RoleUtil.isStudent());
    }
}
