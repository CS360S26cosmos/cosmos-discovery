package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.User;

public class RoleUtil {

    private static User sCurrentUser;

    public static void setCurrentUser(User user) { sCurrentUser = user; }
    public static User getCurrentUser()          { return sCurrentUser; }

    public static boolean isAdmin() {
        return sCurrentUser != null
                && User.ROLE_ADMIN.equals(sCurrentUser.getRole());
    }
    public static boolean isOrganizer() {
        return sCurrentUser != null
                && User.ROLE_ORGANIZER.equals(sCurrentUser.getRole());
    }
    public static boolean isStudent() {
        return sCurrentUser != null
                && User.ROLE_STUDENT.equals(sCurrentUser.getRole());
    }
    public static void clear() { sCurrentUser = null; }
}