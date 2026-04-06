package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;

/**
 * UI helpers for mapping {@link Event#getStatus()} to display text and colors.
 *
 * Kept in a pure utility class so it can be unit tested without Android UI classes.
 */
public final class EventStatusUtil {

    private EventStatusUtil() {}

    public static String toDisplayText(String status) {
        if (Event.STATUS_APPROVED.equals(status)) return "Approved";
        if (Event.STATUS_REJECTED.equals(status)) return "Rejected";
        return "Pending";
    }

    /** Returns a color resource ID for the given status. */
    public static int toColorRes(String status) {
        if (Event.STATUS_APPROVED.equals(status)) return R.color.color_approved;
        if (Event.STATUS_REJECTED.equals(status)) return R.color.color_rejected;
        return R.color.color_pending;
    }
}

