package com.example.cosmos_discovery.ui.student;

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
 * Espresso UI tests for MyEventsFragment (shown inside StudentActivity on the My Events tab).
 * Covers acceptance criteria:
 *   - My Events tab shows all RSVP'd events
 *   - Events grouped into Upcoming and Past
 *
 * Assumes the app is already signed in before these tests run.
 */
@RunWith(AndroidJUnit4.class)
public class MyEventsEspressoTest {

    private ActivityScenario<StudentActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(StudentActivity.class);
        onView(withId(R.id.navMyEvents)).perform(click());
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    /**
     * Verifies the My Events tab is reachable from the bottom nav.
     * Covers: "My Events tab shows all RSVP'd events"
     */
    @Test
    public void myEventsTab_isAccessibleFromBottomNav() {
        onView(withId(R.id.recyclerViewUpcoming))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * Verifies the Upcoming events RecyclerView is displayed on the My Events tab.
     * Covers: "Events grouped into Upcoming and Past"
     */
    @Test
    public void myEventsScreen_showsUpcomingSection() {
        onView(withId(R.id.recyclerViewUpcoming))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * Verifies the Past events RecyclerView is displayed on the My Events tab.
     * Covers: "Events grouped into Upcoming and Past"
     */
    @Test
    public void myEventsScreen_showsPastSection() {
        onView(withId(R.id.recyclerViewPast))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * After 3 seconds, either the upcoming list has items or the empty state is shown.
     * Covers: "My Events tab shows all RSVP'd events"
     */
    @Test
    public void myEventsScreen_upcomingSectionResolvesWithin3Seconds() throws InterruptedException {
        Thread.sleep(3000);
        onView(withId(R.id.recyclerViewUpcoming)).check((view, e) -> {
            if (e != null) throw e;
            android.widget.TextView emptyState =
                    view.getRootView().findViewById(R.id.tvEmptyUpcoming);
            boolean emptyVisible = emptyState != null &&
                    emptyState.getVisibility() == android.view.View.VISIBLE;
            RecyclerView rv = (RecyclerView) view;
            int count = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
            assertTrue("Upcoming section should show events or empty state after 3 seconds",
                    count > 0 || emptyVisible);
        });
    }

    /**
     * After 3 seconds, either the past list has items or the empty state is shown.
     * Covers: "Events grouped into Upcoming and Past"
     */
    @Test
    public void myEventsScreen_pastSectionResolvesWithin3Seconds() throws InterruptedException {
        Thread.sleep(3000);
        onView(withId(R.id.recyclerViewPast)).check((view, e) -> {
            if (e != null) throw e;
            android.widget.TextView emptyState =
                    view.getRootView().findViewById(R.id.tvEmptyPast);
            boolean emptyVisible = emptyState != null &&
                    emptyState.getVisibility() == android.view.View.VISIBLE;
            RecyclerView rv = (RecyclerView) view;
            int count = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
            assertTrue("Past section should show events or empty state after 3 seconds",
                    count > 0 || emptyVisible);
        });
    }
}
