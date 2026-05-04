package com.example.cosmos_discovery.util;

/**
 * Formats a past timestamp as a coarse human-readable "time ago" string.
 *
 * <p>Used by the notification feed and event card timestamps. Output buckets:
 * "Just now", "X min ago", "1 hour ago", "X hours ago", "Yesterday",
 * "X days ago", "1 week ago", "X weeks ago".
 */
public class TimeAgoUtil {

    private TimeAgoUtil() {}

    /**
     * Returns a coarse relative-time description of {@code timestampMillis}
     * compared to the current wall-clock time.
     *
     * @param timestampMillis past time in milliseconds since epoch
     * @return a human-readable relative-time string (e.g. {@code "5 min ago"})
     */
    public static String format(long timestampMillis) {
        long diff    = System.currentTimeMillis() - timestampMillis;
        long seconds = diff / 1_000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long days    = hours   / 24;
        long weeks   = days    / 7;

        if (seconds < 60)  return "Just now";
        if (minutes < 60)  return minutes + " min ago";
        if (hours   <  2)  return "1 hour ago";
        if (hours   < 24)  return hours + " hours ago";
        if (days    ==  1) return "Yesterday";
        if (days    <  7)  return days + " days ago";
        if (weeks   ==  1) return "1 week ago";
        return weeks + " weeks ago";
    }
}
