package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java utility for splitting and sorting RSVPed event lists.
 * Extracted from MyEventsFragment so logic can be unit-tested without Android dependencies.
 */
public class EventSorter {

    /**
     * Returns approved events with dateTime >= now, sorted soonest-first.
     */
    public static List<Event> upcoming(List<Event> events, long now) {
        List<Event> result = new ArrayList<>();
        for (Event e : events) {
            if (!Event.STATUS_APPROVED.equals(e.getStatus())) continue;
            if (e.getDateTime() >= now) result.add(e);
        }
        result.sort((a, b) -> Long.compare(a.getDateTime(), b.getDateTime()));
        return result;
    }

    /**
     * Returns approved events with dateTime < now, sorted most-recent-first.
     */
    public static List<Event> past(List<Event> events, long now) {
        List<Event> result = new ArrayList<>();
        for (Event e : events) {
            if (!Event.STATUS_APPROVED.equals(e.getStatus())) continue;
            if (e.getDateTime() < now) result.add(e);
        }
        result.sort((a, b) -> Long.compare(b.getDateTime(), a.getDateTime()));
        return result;
    }
}
