package com.example.cosmos_discovery.util;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.Menu;
import android.view.MenuItem;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link OrganizerEventMenuHelper}.
 *
 * Verifies the visibility rules for the organizer's overflow menu across the
 * combinations of event status (pending / approved / rejected / cancelled) and
 * whether the event is upcoming or in the past.
 */
public class OrganizerEventMenuHelperTest {

    private static final long FUTURE_MS = System.currentTimeMillis() + 1_000_000_000L;
    private static final long PAST_MS   = System.currentTimeMillis() - 1_000_000_000L;

    private Menu menu;
    private Map<Integer, MenuItem> items;

    @Before
    public void setUp() {
        menu = mock(Menu.class);
        items = new HashMap<>();
        stub(R.id.action_edit);
        stub(R.id.action_stats);
        stub(R.id.action_attendee_list);
        stub(R.id.action_announcement);
        stub(R.id.action_cancel);
        stub(R.id.action_delete);

        when(menu.findItem(anyInt())).thenAnswer(inv -> {
            int id = inv.getArgument(0);
            return items.computeIfAbsent(id, k -> mock(MenuItem.class));
        });
    }

    private void stub(int id) {
        items.put(id, mock(MenuItem.class));
    }

    private static Event event(String status, long dateTime) {
        Event e = new Event("t", dateTime, "loc", null, null, "org");
        e.setStatus(status);
        return e;
    }

    private MenuItem item(int id) {
        return items.get(id);
    }

    // ── Always-visible items ──────────────────────────────────────────────

    @Test
    public void edit_stats_attendee_alwaysVisible_forAnyEvent() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_APPROVED, FUTURE_MS));
        verify(item(R.id.action_edit)).setVisible(true);
        verify(item(R.id.action_stats)).setVisible(true);
        verify(item(R.id.action_attendee_list)).setVisible(true);
    }

    // ── Approved + upcoming → announcement & cancel visible, delete hidden ─

    @Test
    public void approvedUpcoming_showsAnnouncementAndCancel_hidesDelete() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_APPROVED, FUTURE_MS));
        verify(item(R.id.action_announcement)).setVisible(true);
        verify(item(R.id.action_cancel)).setVisible(true);
        verify(item(R.id.action_delete)).setVisible(false);
    }

    // ── Approved + past → announcement & cancel hidden ────────────────────

    @Test
    public void approvedPast_hidesAnnouncementAndCancel_hidesDelete() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_APPROVED, PAST_MS));
        verify(item(R.id.action_announcement)).setVisible(false);
        verify(item(R.id.action_cancel)).setVisible(false);
        verify(item(R.id.action_delete)).setVisible(false);
    }

    // ── Pending → delete visible, others hidden ───────────────────────────

    @Test
    public void pending_showsDelete_hidesAnnouncementAndCancel() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_PENDING, FUTURE_MS));
        verify(item(R.id.action_delete)).setVisible(true);
        verify(item(R.id.action_announcement)).setVisible(false);
        verify(item(R.id.action_cancel)).setVisible(false);
    }

    // ── Rejected → delete visible, others hidden ──────────────────────────

    @Test
    public void rejected_showsDelete_hidesAnnouncementAndCancel() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_REJECTED, FUTURE_MS));
        verify(item(R.id.action_delete)).setVisible(true);
        verify(item(R.id.action_announcement)).setVisible(false);
        verify(item(R.id.action_cancel)).setVisible(false);
    }

    // ── Cancelled → all conditional items hidden ──────────────────────────

    @Test
    public void cancelled_hidesAllConditionalItems() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_CANCELLED, FUTURE_MS));
        verify(item(R.id.action_announcement)).setVisible(false);
        verify(item(R.id.action_cancel)).setVisible(false);
        verify(item(R.id.action_delete)).setVisible(false);
    }

    // ── Null event → conditional items hidden, base items still visible ──

    @Test
    public void nullEvent_basicsVisible_conditionalsHidden() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(menu, null);
        verify(item(R.id.action_edit)).setVisible(true);
        verify(item(R.id.action_stats)).setVisible(true);
        verify(item(R.id.action_attendee_list)).setVisible(true);
        verify(item(R.id.action_announcement)).setVisible(false);
        verify(item(R.id.action_cancel)).setVisible(false);
        verify(item(R.id.action_delete)).setVisible(false);
    }

    // ── findItem is called exactly once per id ────────────────────────────

    @Test
    public void everyMenuItemQueriedOnce() {
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(
                menu, event(Event.STATUS_APPROVED, FUTURE_MS));
        verify(menu, times(1)).findItem(R.id.action_edit);
        verify(menu, times(1)).findItem(R.id.action_stats);
        verify(menu, times(1)).findItem(R.id.action_attendee_list);
        verify(menu, times(1)).findItem(R.id.action_announcement);
        verify(menu, times(1)).findItem(R.id.action_cancel);
        verify(menu, times(1)).findItem(R.id.action_delete);
    }
}
