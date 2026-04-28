package com.example.cosmos_discovery.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class CalendarUtilTest {

    // ── parseEndTime ─────────────────────────────────────────────────────────

    @Test
    public void parseEndTime_validEndTime_returnsCorrectMs() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mma", Locale.US);
        long startMs = sdf.parse("2026-04-06 5:00PM").getTime();
        long expectedEndMs = sdf.parse("2026-04-06 7:00PM").getTime();

        long result = CalendarUtil.parseEndTime("2026-04-06", "7:00pm", startMs);

        assertEquals(expectedEndMs, result);
    }

    @Test
    public void parseEndTime_nullEndTime_returnsOneHourFallback() throws Exception {
        long startMs = 1_000_000_000L;

        long result = CalendarUtil.parseEndTime("2026-04-06", null, startMs);

        assertEquals(startMs + 3_600_000L, result);
    }

    @Test
    public void parseEndTime_emptyEndTime_returnsOneHourFallback() {
        long startMs = 1_000_000_000L;

        long result = CalendarUtil.parseEndTime("2026-04-06", "", startMs);

        assertEquals(startMs + 3_600_000L, result);
    }

    @Test
    public void parseEndTime_malformedEndTime_returnsOneHourFallback() {
        long startMs = 1_000_000_000L;

        long result = CalendarUtil.parseEndTime("2026-04-06", "not-a-time", startMs);

        assertEquals(startMs + 3_600_000L, result);
    }

    @Test
    public void parseEndTime_midnightCrossover_addsDay() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mma", Locale.US);
        long startMs = sdf.parse("2026-04-06 11:00PM").getTime();
        long expectedEndMs = sdf.parse("2026-04-06 1:00AM").getTime() + 86_400_000L;

        long result = CalendarUtil.parseEndTime("2026-04-06", "1:00am", startMs);

        assertEquals(expectedEndMs, result);
    }
}
