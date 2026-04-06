package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EventStatusUtilTest {

    @Test
    public void toDisplayText_approved() {
        assertEquals("Approved", EventStatusUtil.toDisplayText(Event.STATUS_APPROVED));
    }

    @Test
    public void toDisplayText_pending_default() {
        assertEquals("Pending", EventStatusUtil.toDisplayText(Event.STATUS_PENDING));
        assertEquals("Pending", EventStatusUtil.toDisplayText(null));
        assertEquals("Pending", EventStatusUtil.toDisplayText("unknown"));
    }

    @Test
    public void toDisplayText_rejected() {
        assertEquals("Rejected", EventStatusUtil.toDisplayText(Event.STATUS_REJECTED));
    }

    @Test
    public void toColorRes_matchesStatus() {
        assertEquals(R.color.color_approved, EventStatusUtil.toColorRes(Event.STATUS_APPROVED));
        assertEquals(R.color.color_pending, EventStatusUtil.toColorRes(Event.STATUS_PENDING));
        assertEquals(R.color.color_rejected, EventStatusUtil.toColorRes(Event.STATUS_REJECTED));
    }
}

