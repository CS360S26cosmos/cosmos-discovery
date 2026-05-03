package com.example.cosmos_discovery.model;

import java.util.List;

/**
 * Immutable snapshot of platform-wide metrics shown on the admin dashboard.
 *
 * Use {@link #compute(List, int, long, long)} to build an instance from raw event + user data.
 * The static factory is pure Java (no Firebase / Android deps) so it can be unit-tested directly.
 */
public class DashboardMetrics {

    public final int activeEvents;   // approved events whose dateTime is in the future
    public final int pendingEvents;  // events with status == pending
    public final int pastEvents;     // approved events whose dateTime has already passed
    public final int totalUsers;     // platform-wide user count (ignores date range)
    public final int totalRsvps;     // sum of rsvpCount for all in-range approved events
    public final int recentEvents;   // events with createdAt in the last 30 days (ignores date range filter)

    public DashboardMetrics(int activeEvents, int pendingEvents, int pastEvents,
                             int totalUsers, int totalRsvps, int recentEvents) {
        this.activeEvents  = activeEvents;
        this.pendingEvents = pendingEvents;
        this.pastEvents    = pastEvents;
        this.totalUsers    = totalUsers;
        this.totalRsvps    = totalRsvps;
        this.recentEvents  = recentEvents;
    }

    /**
     * Compute metrics from a raw event list and user count.
     *
     * @param events    all events from Firestore (unfiltered)
     * @param userCount total number of user documents
     * @param fromMs    start of date range filter (epoch ms, inclusive); pass 0 for all-time
     * @param toMs      end of date range filter (epoch ms, inclusive); pass Long.MAX_VALUE for all-time
     */
    public static DashboardMetrics compute(List<Event> events, int userCount,
                                           long fromMs, long toMs) {
        long now = System.currentTimeMillis();
        long thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000;
        int active = 0, pending = 0, past = 0, rsvps = 0, recent = 0;

        for (Event e : events) {
            // recentEvents: based on createdAt, independent of the date range filter
            long ca = e.getCreatedAt();
            if (ca > 0 && ca >= thirtyDaysAgo) recent++;

            long dt = e.getDateTime();
            if (dt < fromMs || dt > toMs) continue;

            String status = e.getStatus();
            if (Event.STATUS_APPROVED.equals(status)) {
                if (dt > now) {
                    active++;
                } else {
                    past++;
                }
                rsvps += e.getRsvpCount();
            } else if (Event.STATUS_PENDING.equals(status)) {
                pending++;
            }
            // rejected events are excluded from all counts
        }

        return new DashboardMetrics(active, pending, past, userCount, rsvps, recent);
    }
}
