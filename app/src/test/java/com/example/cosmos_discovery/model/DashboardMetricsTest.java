package com.example.cosmos_discovery.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DashboardMetricsTest {

    private static final long NOW = System.currentTimeMillis();
    private static final long PAST = NOW - 7 * 24 * 60 * 60 * 1000L;   // 1 week ago
    private static final long FUTURE = NOW + 7 * 24 * 60 * 60 * 1000L; // 1 week from now

    // ── helpers ──────────────────────────────────────────────────────────

    private Event makeEvent(String status, long dateTime, int rsvpCount) {
        Event e = new Event();
        e.setStatus(status);
        e.setDateTime(dateTime);
        e.setRsvpCount(rsvpCount);
        return e;
    }

    private Event makeEventWithCreatedAt(String status, long dateTime, int rsvpCount, long createdAt) {
        Event e = makeEvent(status, dateTime, rsvpCount);
        e.setCreatedAt(createdAt);
        return e;
    }

    // ── all-time filter ──────────────────────────────────────────────────

    @Test
    public void compute_emptyList_allZeros() {
        DashboardMetrics m = DashboardMetrics.compute(Collections.emptyList(), 0, 0, Long.MAX_VALUE);
        assertEquals(0, m.activeEvents);
        assertEquals(0, m.pendingEvents);
        assertEquals(0, m.pastEvents);
        assertEquals(0, m.totalUsers);
        assertEquals(0, m.totalRsvps);
    }

    @Test
    public void compute_mixedStatuses_correctCounts() {
        List<Event> events = Arrays.asList(
                makeEvent(Event.STATUS_APPROVED, FUTURE, 10), // active
                makeEvent(Event.STATUS_APPROVED, FUTURE, 5),  // active
                makeEvent(Event.STATUS_APPROVED, PAST,   20), // past
                makeEvent(Event.STATUS_PENDING,  FUTURE, 0),  // pending
                makeEvent(Event.STATUS_REJECTED, PAST,   3)   // excluded
        );

        DashboardMetrics m = DashboardMetrics.compute(events, 42, 0, Long.MAX_VALUE);

        assertEquals(2, m.activeEvents);
        assertEquals(1, m.pendingEvents);
        assertEquals(1, m.pastEvents);
        assertEquals(42, m.totalUsers);
        assertEquals(35, m.totalRsvps); // 10 + 5 + 20
    }

    @Test
    public void compute_rejectedEventsExcluded() {
        List<Event> events = Arrays.asList(
                makeEvent(Event.STATUS_REJECTED, FUTURE, 100),
                makeEvent(Event.STATUS_REJECTED, PAST,   50)
        );

        DashboardMetrics m = DashboardMetrics.compute(events, 5, 0, Long.MAX_VALUE);

        assertEquals(0, m.activeEvents);
        assertEquals(0, m.pendingEvents);
        assertEquals(0, m.pastEvents);
        assertEquals(0, m.totalRsvps);
    }

    @Test
    public void compute_userCountIgnoresDateRange() {
        DashboardMetrics m = DashboardMetrics.compute(Collections.emptyList(), 99, FUTURE, FUTURE);
        assertEquals(99, m.totalUsers);
    }

    // ── date range filter ─────────────────────────────────────────────────

    @Test
    public void compute_dateRangeFiltersEvents() {
        long rangeStart = NOW - 3 * 24 * 60 * 60 * 1000L; // 3 days ago
        long rangeEnd   = NOW + 3 * 24 * 60 * 60 * 1000L; // 3 days from now

        List<Event> events = Arrays.asList(
                makeEvent(Event.STATUS_APPROVED, PAST,   99),  // outside range (1 week ago)
                makeEvent(Event.STATUS_APPROVED, FUTURE, 99),  // outside range (1 week from now)
                makeEvent(Event.STATUS_APPROVED, NOW - 24 * 60 * 60 * 1000L, 10), // in range, past
                makeEvent(Event.STATUS_APPROVED, NOW + 24 * 60 * 60 * 1000L, 5)   // in range, future
        );

        DashboardMetrics m = DashboardMetrics.compute(events, 10, rangeStart, rangeEnd);

        assertEquals(1, m.activeEvents);
        assertEquals(1, m.pastEvents);
        assertEquals(15, m.totalRsvps);
    }

    @Test
    public void compute_recentEvents_countsByCreatedAt() {
        long twoDaysAgo  = NOW - 2 * 24 * 60 * 60 * 1000L;
        long fortyDaysAgo = NOW - 40 * 24 * 60 * 60 * 1000L;

        List<Event> events = Arrays.asList(
                makeEventWithCreatedAt(Event.STATUS_APPROVED, FUTURE, 0, twoDaysAgo),  // recent
                makeEventWithCreatedAt(Event.STATUS_APPROVED, FUTURE, 0, NOW),          // recent
                makeEventWithCreatedAt(Event.STATUS_APPROVED, PAST,   0, fortyDaysAgo) // too old
        );

        DashboardMetrics m = DashboardMetrics.compute(events, 0, 0, Long.MAX_VALUE);
        assertEquals(2, m.recentEvents);
    }

    @Test
    public void compute_noEventsInRange_allZeroExceptUsers() {
        List<Event> events = new ArrayList<>();
        events.add(makeEvent(Event.STATUS_APPROVED, PAST, 50));

        long futureFrom = NOW + 10 * 24 * 60 * 60 * 1000L;
        long futureTo   = NOW + 20 * 24 * 60 * 60 * 1000L;

        DashboardMetrics m = DashboardMetrics.compute(events, 7, futureFrom, futureTo);

        assertEquals(0, m.activeEvents);
        assertEquals(0, m.pendingEvents);
        assertEquals(0, m.pastEvents);
        assertEquals(7, m.totalUsers);
        assertEquals(0, m.totalRsvps);
    }
}
