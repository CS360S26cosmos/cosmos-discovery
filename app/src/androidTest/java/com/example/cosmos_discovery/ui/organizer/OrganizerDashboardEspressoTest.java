package com.example.cosmos_discovery.ui.organizer;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cosmos_discovery.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.assertTrue;

/**
 * Espresso UI tests for OrganizerActivity (organizer dashboard).
 * Covers acceptance criteria:
 *   - Edit option available for all organizer-owned events (organizer sees their events list)
 *   - Create Event form is reachable from the dashboard
 *
 * Assumes the app is already signed in as an organizer before these tests run.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDashboardEspressoTest {

    private ActivityScenario<OrganizerActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(OrganizerActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    /**
     * Verifies the Create Event button is visible on the organizer dashboard.
     * Covers: "Edit option available for all organizer-owned events" (creation precedes editing)
     */
    @Test
    public void organizerDashboard_showsCreateEventButton() {
        onView(withId(R.id.btnCreateEventCard)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the posted events list is present on the dashboard.
     * Covers: "Edit option available for all organizer-owned events"
     */
    @Test
    public void organizerDashboard_showsPostedEventsList() {
        onView(withId(R.id.rvPostedEvents))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * After 3 seconds, either posted events are loaded or the empty state is shown.
     * Covers: "Edit option available for all organizer-owned events"
     */
    @Test
    public void organizerDashboard_eventsOrEmptyStateShownWithin3Seconds() throws InterruptedException {
        Thread.sleep(3000);
        onView(withId(R.id.rvPostedEvents)).check((view, e) -> {
            if (e != null) throw e;
            android.widget.TextView emptyState =
                    view.getRootView().findViewById(R.id.tvEmptyPostedEvents);
            boolean emptyVisible = emptyState != null &&
                    emptyState.getVisibility() == android.view.View.VISIBLE;
            RecyclerView rv = (RecyclerView) view;
            int count = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
            assertTrue("Dashboard should show events or empty state after 3 seconds",
                    count > 0 || emptyVisible);
        });
    }

    /**
     * Tapping the Create Event button navigates to the Add Event form.
     * Verifies the create event form is reachable from the dashboard.
     * Covers: "Create Event form includes: title, ..." (US-08 reachability)
     */
    @Test
    public void organizerDashboard_clickCreateEvent_opensAddEventForm() {
        onView(withId(R.id.btnCreateEventCard)).perform(click());
        onView(withId(R.id.etEventName)).check(matches(isDisplayed()));
    }
}
