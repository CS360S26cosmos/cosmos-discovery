package com.example.cosmos_discovery.util;

import android.view.Menu;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;

/**
 * Single source of truth for which items appear in the organizer's
 * three-dot menu, used by both EventDetailsActivity and OrganizerEventAdapter.
 */
public final class OrganizerEventMenuHelper {

    private OrganizerEventMenuHelper() {}

    public static void applyOrganizerMenuVisibility(Menu menu, Event event) {
        boolean approved  = event != null && Event.STATUS_APPROVED.equals(event.getStatus());
        boolean pending   = event != null && Event.STATUS_PENDING.equals(event.getStatus());
        boolean rejected  = event != null && Event.STATUS_REJECTED.equals(event.getStatus());
        boolean cancelled = event != null && Event.STATUS_CANCELLED.equals(event.getStatus());
        boolean upcoming  = event != null && event.getDateTime() > System.currentTimeMillis();

        boolean showAnnouncement = approved && upcoming && !cancelled;
        boolean showCancel       = approved && upcoming && !cancelled;
        boolean showDelete       = pending || rejected;

        menu.findItem(R.id.action_edit).setVisible(true);
        menu.findItem(R.id.action_stats).setVisible(true);
        menu.findItem(R.id.action_attendee_list).setVisible(true);
        menu.findItem(R.id.action_announcement).setVisible(showAnnouncement);
        menu.findItem(R.id.action_cancel).setVisible(showCancel);
        menu.findItem(R.id.action_delete).setVisible(showDelete);
    }
}
