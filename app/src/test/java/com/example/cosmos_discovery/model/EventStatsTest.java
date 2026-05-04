package com.example.cosmos_discovery.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventStatsTest {

    private static final float DELTA = 0.001f;

    // ── helpers ──────────────────────────────────────────────────────────

    private Event makeEvent(int rsvpCount, int capacity) {
        Event e = new Event();
        e.setRsvpCount(rsvpCount);
        e.setCapacity(capacity);
        return e;
    }

    private Event makeEventWithCheckins(int rsvpCount, int capacity, String... checkinIds) {
        Event e = makeEvent(rsvpCount, capacity);
        e.setCheckedInIds(Arrays.asList(checkinIds));
        return e;
    }

    // ── rsvpCount ─────────────────────────────────────────────────────────

    @Test
    public void compute_rsvpCount_matchesEvent() {
        EventStats s = EventStats.compute(makeEvent(7, 50));
        assertEquals(7, s.rsvpCount);
    }

    @Test
    public void compute_zeroRsvps_zeroRsvpCount() {
        EventStats s = EventStats.compute(makeEvent(0, 50));
        assertEquals(0, s.rsvpCount);
    }

    // ── capacity & unlimitedCapacity ──────────────────────────────────────

    @Test
    public void compute_limitedCapacity_notUnlimited() {
        EventStats s = EventStats.compute(makeEvent(5, 40));
        assertFalse(s.unlimitedCapacity);
        assertEquals(40, s.capacity);
    }

    @Test
    public void compute_zeroCapacity_markedUnlimited() {
        EventStats s = EventStats.compute(makeEvent(5, 0));
        assertTrue(s.unlimitedCapacity);
        assertEquals(0, s.capacity);
    }

    // ── fillRatePercent ───────────────────────────────────────────────────

    @Test
    public void compute_fillRate_calculatedCorrectly() {
        EventStats s = EventStats.compute(makeEvent(1, 40));
        assertEquals(3, s.fillRatePercent); // round(1*100/40) = round(2.5) = 3
    }

    @Test
    public void compute_fillRate_zeroRsvps_zeroPercent() {
        EventStats s = EventStats.compute(makeEvent(0, 100));
        assertEquals(0, s.fillRatePercent);
    }

    @Test
    public void compute_fillRate_fullCapacity_hundredPercent() {
        EventStats s = EventStats.compute(makeEvent(50, 50));
        assertEquals(100, s.fillRatePercent);
    }

    @Test
    public void compute_fillRate_unlimitedCapacity_alwaysZero() {
        EventStats s = EventStats.compute(makeEvent(99, 0));
        assertEquals(0, s.fillRatePercent);
    }

    @Test
    public void compute_fillRate_roundsHalfUp() {
        // 5/10 = 50.0% — no rounding ambiguity
        EventStats s = EventStats.compute(makeEvent(5, 10));
        assertEquals(50, s.fillRatePercent);
    }

    @Test
    public void compute_fillRate_partialRoundsCorrectly() {
        // 2/3 = 66.67% → rounds to 67
        EventStats s = EventStats.compute(makeEvent(2, 3));
        assertEquals(67, s.fillRatePercent);
    }

    // ── checkinCount ──────────────────────────────────────────────────────

    @Test
    public void compute_nullCheckedInIds_zeroCheckins() {
        Event e = makeEvent(5, 50);
        // checkedInIds left as null (default)
        EventStats s = EventStats.compute(e);
        assertEquals(0, s.checkinCount);
    }

    @Test
    public void compute_emptyCheckedInIds_zeroCheckins() {
        Event e = makeEvent(5, 50);
        e.setCheckedInIds(Collections.emptyList());
        EventStats s = EventStats.compute(e);
        assertEquals(0, s.checkinCount);
    }

    @Test
    public void compute_checkinCount_matchesListSize() {
        EventStats s = EventStats.compute(makeEventWithCheckins(3, 50, "u1", "u2"));
        assertEquals(2, s.checkinCount);
    }

    // ── bar fractions ─────────────────────────────────────────────────────

    @Test
    public void compute_bothZero_zeroBarFractions() {
        EventStats s = EventStats.compute(makeEvent(0, 50));
        assertEquals(0f, s.rsvpBarFraction,    DELTA);
        assertEquals(0f, s.checkinBarFraction, DELTA);
    }

    @Test
    public void compute_noCheckins_rsvpBarIsFullHeight() {
        EventStats s = EventStats.compute(makeEventWithCheckins(5, 50));
        assertEquals(1.0f, s.rsvpBarFraction,  DELTA);
        assertEquals(0.0f, s.checkinBarFraction, DELTA);
    }

    @Test
    public void compute_noRsvps_checkinBarIsFullHeight() {
        Event e = makeEvent(0, 50);
        e.setCheckedInIds(Arrays.asList("u1", "u2", "u3"));
        EventStats s = EventStats.compute(e);
        assertEquals(0.0f, s.rsvpBarFraction,    DELTA);
        assertEquals(1.0f, s.checkinBarFraction, DELTA);
    }

    @Test
    public void compute_equalRsvpsAndCheckins_bothFullHeight() {
        EventStats s = EventStats.compute(makeEventWithCheckins(4, 50, "u1", "u2", "u3", "u4"));
        assertEquals(1.0f, s.rsvpBarFraction,    DELTA);
        assertEquals(1.0f, s.checkinBarFraction, DELTA);
    }

    @Test
    public void compute_rsvpHigher_checkinFractionIsRatio() {
        // rsvps=8, checkins=4 → checkin bar = 4/8 = 0.5
        EventStats s = EventStats.compute(makeEventWithCheckins(8, 50, "u1", "u2", "u3", "u4"));
        assertEquals(1.0f, s.rsvpBarFraction,    DELTA);
        assertEquals(0.5f, s.checkinBarFraction, DELTA);
    }

    @Test
    public void compute_checkinHigher_rsvpFractionIsRatio() {
        // rsvps=3, checkins=6 → rsvp bar = 3/6 = 0.5
        Event e = makeEvent(3, 50);
        e.setCheckedInIds(Arrays.asList("u1", "u2", "u3", "u4", "u5", "u6"));
        EventStats s = EventStats.compute(e);
        assertEquals(0.5f, s.rsvpBarFraction,    DELTA);
        assertEquals(1.0f, s.checkinBarFraction, DELTA);
    }
}
