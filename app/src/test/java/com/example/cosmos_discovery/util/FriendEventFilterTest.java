package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class FriendEventFilterTest {

    private Event eventWithAttendees(String... uids) {
        Event e = new Event("title", 0L, "loc", null, null, null);
        e.setAttendeeIds(new ArrayList<>(Arrays.asList(uids)));
        return e;
    }

    @Test
    public void filterByFriends_friendAttending_included() {
        Event e = eventWithAttendees("uid-friend");
        Set<String> friends = new HashSet<>(Collections.singletonList("uid-friend"));
        assertEquals(1, FriendEventFilter.filterByFriends(Collections.singletonList(e), friends).size());
    }

    @Test
    public void filterByFriends_noFriendAttending_excluded() {
        Event e = eventWithAttendees("uid-stranger");
        Set<String> friends = new HashSet<>(Collections.singletonList("uid-friend"));
        assertEquals(0, FriendEventFilter.filterByFriends(Collections.singletonList(e), friends).size());
    }

    @Test
    public void filterByFriends_emptyFriendSet_returnsEmpty() {
        Event e = eventWithAttendees("uid-anyone");
        assertEquals(0, FriendEventFilter.filterByFriends(Collections.singletonList(e), new HashSet<>()).size());
    }

    @Test
    public void filterByFriends_nullAttendeeIds_excluded() {
        // Event created with null attendeeIds (not set via setAttendeeIds)
        Event e = new Event("title", 0L, "loc", null, null, null);
        Set<String> friends = new HashSet<>(Collections.singletonList("uid-friend"));
        assertEquals(0, FriendEventFilter.filterByFriends(Collections.singletonList(e), friends).size());
    }

    @Test
    public void filterByFriends_oneFriendAmongManyAttendees_included() {
        Event e = eventWithAttendees("uid-stranger", "uid-friend", "uid-other");
        Set<String> friends = new HashSet<>(Collections.singletonList("uid-friend"));
        assertEquals(1, FriendEventFilter.filterByFriends(Collections.singletonList(e), friends).size());
    }

    @Test
    public void filterByFriends_emptyEventList_returnsEmpty() {
        Set<String> friends = new HashSet<>(Collections.singletonList("uid-friend"));
        assertEquals(0, FriendEventFilter.filterByFriends(Collections.emptyList(), friends).size());
    }
}
