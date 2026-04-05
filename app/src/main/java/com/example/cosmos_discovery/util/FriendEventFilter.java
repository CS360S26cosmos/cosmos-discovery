package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure-Java utility for filtering events by friend attendance.
 * Extracted from FriendsFragment so logic can be unit-tested without Android dependencies.
 */
public class FriendEventFilter {

    /**
     * Returns only events where at least one attendee UID is in {@code friendUids}.
     */
    public static List<Event> filterByFriends(List<Event> events, Set<String> friendUids) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (event.getAttendeeIds() == null) continue;
            for (String uid : event.getAttendeeIds()) {
                if (friendUids.contains(uid)) {
                    result.add(event);
                    break;
                }
            }
        }
        return result;
    }
}
