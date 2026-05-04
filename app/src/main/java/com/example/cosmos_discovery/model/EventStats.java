package com.example.cosmos_discovery.model;

import java.util.List;

/**
 * Immutable snapshot of computed statistics for a single event.
 *
 * Use {@link #compute(Event)} to build an instance from an Event.
 * The static factory is pure Java (no Firebase / Android deps) so it can be unit-tested directly.
 */
public class EventStats {

    /** Number of RSVPs on the event. */
    public final int rsvpCount;

    /** Raw capacity value from the event (0 = unlimited). */
    public final int capacity;

    /** Number of users who checked in. */
    public final int checkinCount;

    /**
     * RSVP fill rate as a percentage (0–100), rounded to the nearest integer.
     * Always 0 when capacity is unlimited.
     */
    public final int fillRatePercent;

    /** True when capacity == 0, meaning no cap was set. */
    public final boolean unlimitedCapacity;

    /**
     * Relative height of the RSVP bar (0.0–1.0) compared to the taller of the two bars.
     * 0.0 when both rsvpCount and checkinCount are zero.
     */
    public final float rsvpBarFraction;

    /**
     * Relative height of the check-in bar (0.0–1.0) compared to the taller of the two bars.
     * 0.0 when both rsvpCount and checkinCount are zero.
     */
    public final float checkinBarFraction;

    public EventStats(int rsvpCount, int capacity, int checkinCount,
                      int fillRatePercent, boolean unlimitedCapacity,
                      float rsvpBarFraction, float checkinBarFraction) {
        this.rsvpCount        = rsvpCount;
        this.capacity         = capacity;
        this.checkinCount     = checkinCount;
        this.fillRatePercent  = fillRatePercent;
        this.unlimitedCapacity = unlimitedCapacity;
        this.rsvpBarFraction  = rsvpBarFraction;
        this.checkinBarFraction = checkinBarFraction;
    }

    /**
     * Compute stats from a single event.
     *
     * @param event the event to compute stats for
     */
    public static EventStats compute(Event event) {
        int rsvps    = event.getRsvpCount();
        int cap      = event.getCapacity();
        List<String> checkedIn = event.getCheckedInIds();
        int checkins = checkedIn != null ? checkedIn.size() : 0;

        boolean unlimited = cap == 0;
        int fillRate = unlimited ? 0 : (int) Math.round(rsvps * 100.0 / cap);

        int maxVal = Math.max(rsvps, checkins);
        float rsvpFraction    = maxVal > 0 ? (float) rsvps    / maxVal : 0f;
        float checkinFraction = maxVal > 0 ? (float) checkins / maxVal : 0f;

        return new EventStats(rsvps, cap, checkins, fillRate, unlimited,
                rsvpFraction, checkinFraction);
    }
}
