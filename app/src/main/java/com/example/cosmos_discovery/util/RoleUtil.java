package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.User;

/**
 * In-memory singleton holding the currently signed-in {@link com.example.cosmos_discovery.model.User}
 * and providing convenience role-check methods.
 *
 * Call {@link #setCurrentUser(com.example.cosmos_discovery.model.User)} after a successful
 * sign-in or app resume, and {@link #clear()} on sign-out.
 */
public class RoleUtil {

    private static User sCurrentUser;

    /** Sets the globally available current user after sign-in. */
    public static void setCurrentUser(User user) { sCurrentUser = user; }

    /** Returns the currently signed-in user, or {@code null} if not signed in. */
    public static User getCurrentUser()          { return sCurrentUser; }

    /** Returns {@code true} if the current user has the admin role. */
    public static boolean isAdmin() {
        return sCurrentUser != null
                && User.ROLE_ADMIN.equals(sCurrentUser.getRole());
    }
    /** Returns {@code true} if the current user has the organizer role. */
    public static boolean isOrganizer() {
        return sCurrentUser != null
                && User.ROLE_ORGANIZER.equals(sCurrentUser.getRole());
    }
    /** Returns {@code true} if the current user has the student role. */
    public static boolean isStudent() {
        return sCurrentUser != null
                && User.ROLE_STUDENT.equals(sCurrentUser.getRole());
    }
    /** Clears the current user. Call this on sign-out. */
    public static void clear() { sCurrentUser = null; }
}