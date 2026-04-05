package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EventSorterTest {

    private Event approved(long dateTime) {
        Event e = new Event("title", dateTime, "loc", null, null, null);
        e.setStatus(Event.STATUS_APPROVED);
        return e;
    }

    private Event pending(long dateTime) {
        Event e = new Event("title", dateTime, "loc", null, null, null);
        e.setStatus(Event.STATUS_PENDING);
        return e;
    }

    // ── upcoming ────────────────────────────────────────────────────────────

    @Test
    public void upcoming_futureApprovedEvent_included() {
        long now = System.currentTimeMillis();
        assertEquals(1, EventSorter.upcoming(Collections.singletonList(approved(now + 10_000)), now).size());
    }

    @Test
    public void upcoming_pastEvent_excluded() {
        long now = System.currentTimeMillis();
        assertEquals(0, EventSorter.upcoming(Collections.singletonList(approved(now - 10_000)), now).size());
    }

    @Test
    public void upcoming_pendingFutureEvent_excluded() {
        long now = System.currentTimeMillis();
        assertEquals(0, EventSorter.upcoming(Collections.singletonList(pending(now + 10_000)), now).size());
    }

    @Test
    public void upcoming_sortedByDateAscending() {
        long now = System.currentTimeMillis();
        Event first  = approved(now + 1_000);
        Event second = approved(now + 2_000);
        List<Event> result = EventSorter.upcoming(Arrays.asList(second, first), now);
        assertEquals(2, result.size());
        assertEquals(first.getDateTime(), result.get(0).getDateTime());
    }

    @Test
    public void upcoming_emptyInput_returnsEmpty() {
        assertEquals(0, EventSorter.upcoming(Collections.emptyList(), System.currentTimeMillis()).size());
    }

    // ── past ────────────────────────────────────────────────────────────────

    @Test
    public void past_pastApprovedEvent_included() {
        long now = System.currentTimeMillis();
        assertEquals(1, EventSorter.past(Collections.singletonList(approved(now - 10_000)), now).size());
    }

    @Test
    public void past_futureEvent_excluded() {
        long now = System.currentTimeMillis();
        assertEquals(0, EventSorter.past(Collections.singletonList(approved(now + 10_000)), now).size());
    }

    @Test
    public void past_pendingPastEvent_excluded() {
        long now = System.currentTimeMillis();
        assertEquals(0, EventSorter.past(Collections.singletonList(pending(now - 10_000)), now).size());
    }

    @Test
    public void past_sortedByDateDescending() {
        long now = System.currentTimeMillis();
        Event recent = approved(now - 1_000);
        Event older  = approved(now - 2_000);
        List<Event> result = EventSorter.past(Arrays.asList(older, recent), now);
        assertEquals(2, result.size());
        assertEquals(recent.getDateTime(), result.get(0).getDateTime());
    }

    @Test
    public void past_emptyInput_returnsEmpty() {
        assertEquals(0, EventSorter.past(Collections.emptyList(), System.currentTimeMillis()).size());
    }
}
