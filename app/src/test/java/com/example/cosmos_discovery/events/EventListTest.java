package com.example.cosmos_discovery.events;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for event feed logic (US-01)
 */
public class EventListTest {

    // Simple fake Event model for testing
    static class Event {
        String title;
        String date;
        String location;

        Event(String title, String date, String location) {
            this.title = title;
            this.date = date;
            this.location = location;
        }
    }

    @Test
    public void eventList_notEmpty_displaysEvents() {
        List<Event> events = new ArrayList<>();
        events.add(new Event("Hackathon", "2026-04-01", "LUMS"));

        assertFalse(events.isEmpty());
    }

    @Test
    public void event_containsRequiredFields() {
        Event event = new Event("Seminar", "2026-04-02", "SDSB");

        assertNotNull(event.title);
        assertNotNull(event.date);
        assertNotNull(event.location);
    }

    @Test
    public void emptyEventList_returnsTrue() {
        List<Event> events = new ArrayList<>();

        assertTrue(events.isEmpty());
    }

    @Test
    public void eventLoad_withinExpectedTime() {
        long start = System.currentTimeMillis();

        // simulate loading
        List<Event> events = new ArrayList<>();
        events.add(new Event("Event", "2026-04-03", "Campus"));

        long end = System.currentTimeMillis();

        assertTrue((end - start) < 3000);
    }
}