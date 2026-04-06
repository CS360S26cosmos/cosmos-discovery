package com.example.cosmos_discovery.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventFormUtilTest {

    private Locale mPrev;

    @Before
    public void setUp() {
        mPrev = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown() {
        Locale.setDefault(mPrev);
    }

    @Test
    public void parseDateTimeMs_isoDateAndAmPmTime_parses() {
        long ms = EventFormUtil.parseDateTimeMs("2026-04-06", "5:00pm");
        assertTrue(ms > 0);
    }

    @Test
    public void parseDateTimeMs_24HourTime_parses() {
        long ms = EventFormUtil.parseDateTimeMs("2026-04-06", "17:00");
        assertTrue(ms > 0);
    }

    @Test
    public void parseDateTimeMs_invalid_returnsMinusOne() {
        assertEquals(-1L, EventFormUtil.parseDateTimeMs("not-a-date", "nope"));
        assertEquals(-1L, EventFormUtil.parseDateTimeMs("", "5:00pm"));
        assertEquals(-1L, EventFormUtil.parseDateTimeMs("2026-04-06", ""));
    }

    @Test
    public void normalizeAccessType_lumsOnly() {
        assertEquals("lums_only", EventFormUtil.normalizeAccessType("LUMS Only"));
        assertEquals("lums_only", EventFormUtil.normalizeAccessType("students only"));
        assertEquals("lums_only", EventFormUtil.normalizeAccessType("campus"));
    }

    @Test
    public void normalizeAccessType_open_default() {
        assertEquals("open", EventFormUtil.normalizeAccessType("Open"));
        assertEquals("open", EventFormUtil.normalizeAccessType(""));
        assertEquals("open", EventFormUtil.normalizeAccessType(null));
    }
}

