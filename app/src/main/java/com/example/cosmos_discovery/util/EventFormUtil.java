package com.example.cosmos_discovery.util;

import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parsing and normalization helpers for the organizer Add/Edit Event form.
 *
 * These helpers are pure Java so they can be tested with JVM unit tests.
 */
public final class EventFormUtil {

    private EventFormUtil() {}

    /**
     * Attempts to parse a combined date + time into epoch milliseconds.
     *
     * Accepts common user-entered formats:
     * - Date: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy, MMM d, yyyy, MMMM d, yyyy
     * - Time: H:mm, HH:mm, h:mma, h:mm a, hh:mm a
     *
     * @return epoch ms, or -1 if parsing fails.
     */
    public static long parseDateTimeMs(String dateText, String timeText) {
        String d = safeTrim(dateText);
        String t = safeTrim(timeText);
        if (d.isEmpty() || t.isEmpty()) return -1L;

        List<String> datePatterns = Arrays.asList(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "MMM d, yyyy",
                "MMMM d, yyyy"
        );
        List<String> timePatterns = Arrays.asList(
                "H:mm",
                "HH:mm",
                "h:mma",
                "h:mm a",
                "hh:mm a"
        );

        for (String dp : datePatterns) {
            for (String tp : timePatterns) {
                long ms = tryParse(dp, tp, d, t);
                if (ms != -1L) return ms;
            }
        }
        return -1L;
    }

    private static long tryParse(String datePattern, String timePattern, String dateText, String timeText) {
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern + " " + timePattern, Locale.getDefault());
        sdf.setLenient(false);
        String combined = dateText + " " + timeText;
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse(combined, pos);
        if (parsed != null && pos.getIndex() == combined.length()) {
            return parsed.getTime();
        }
        return -1L;
    }

    /**
     * Normalizes free-form "Open to" text to the canonical Event accessType values.
     *
     * @return "lums_only" or "open".
     */
    public static String normalizeAccessType(String openToText) {
        String s = safeTrim(openToText).toLowerCase(Locale.getDefault());
        if (s.contains("lums") || s.contains("students") || s.contains("campus") || s.contains("only")) {
            return "lums_only";
        }
        return "open";
    }

    public static String accessTypeToDisplay(String accessType) {
        String s = safeTrim(accessType).toLowerCase(Locale.getDefault());
        return "lums_only".equals(s) ? "LUMS Only" : "Open";
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}

