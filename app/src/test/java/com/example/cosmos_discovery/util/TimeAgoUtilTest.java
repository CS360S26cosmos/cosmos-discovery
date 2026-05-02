package com.example.cosmos_discovery.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeAgoUtilTest {

    private static final long NOW = System.currentTimeMillis();

    @Test
    public void justNow() {
        assertEquals("Just now", TimeAgoUtil.format(NOW - 30_000));
    }

    @Test
    public void minutesAgo() {
        assertEquals("5 min ago", TimeAgoUtil.format(NOW - 5 * 60_000));
    }

    @Test
    public void oneHourAgo() {
        assertEquals("1 hour ago", TimeAgoUtil.format(NOW - 60 * 60_000));
    }

    @Test
    public void hoursAgo() {
        assertEquals("3 hours ago", TimeAgoUtil.format(NOW - 3 * 60 * 60_000));
    }

    @Test
    public void yesterday() {
        assertEquals("Yesterday", TimeAgoUtil.format(NOW - 24 * 60 * 60_000));
    }

    @Test
    public void daysAgo() {
        assertEquals("3 days ago", TimeAgoUtil.format(NOW - 3 * 24 * 60 * 60_000L));
    }

    @Test
    public void weeksAgo() {
        assertEquals("1 week ago", TimeAgoUtil.format(NOW - 7 * 24 * 60 * 60_000L));
    }
}
