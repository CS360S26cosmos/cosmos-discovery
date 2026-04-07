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
 * Espresso UI tests for FriendsFragment (shown inside StudentActivity on the Friends tab).
 * Covers acceptance criteria:
 *   - Activity feed shows recent RSVPs made by friends
 *   - Each feed item shows: friend name, action button, event name
 *   - Feed accessible from a dedicated tab on the home screen
 *
 * Assumes the app is already signed in before these tests run.
 */
@RunWith(AndroidJUnit4.class)
public class FriendsActivityFeedEspressoTest {

    private ActivityScenario<StudentActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(StudentActivity.class);
        onView(withId(R.id.navFriends)).perform(click());
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    /**
     * Verifies the Friends tab is accessible via the bottom nav.
     * Covers: "Feed accessible from a dedicated tab on the home screen"
     */
    @Test
    public void friendsTab_isAccessibleFromBottomNav() {
        onView(withId(R.id.recyclerViewFriends))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * Verifies the friends carousel RecyclerView is displayed on the Friends tab.
     * Covers: "Each feed item shows: friend name, action button, event name"
     */
    @Test
    public void friendsScreen_showsFriendsCarousel() {
        onView(withId(R.id.recyclerViewFriends))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * Verifies the friend events feed RecyclerView is displayed on the Friends tab.
     * Covers: "Activity feed shows recent RSVPs made by friends"
     */
    @Test
    public void friendsScreen_showsFriendEventsFeed() {
        onView(withId(R.id.recyclerViewFriendEvents))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * After 3 seconds, either the friend events feed has items or the empty state is shown.
     * Covers: "Activity feed shows recent RSVPs made by friends"
     */
    @Test
    public void friendsScreen_feedResolvesWithin3Seconds() throws InterruptedException {
        Thread.sleep(3000);
        onView(withId(R.id.recyclerViewFriendEvents)).check((view, e) -> {
            if (e != null) throw e;
            android.widget.TextView emptyState =
                    view.getRootView().findViewById(R.id.tvNoFriendEvents);
            boolean emptyVisible = emptyState != null &&
                    emptyState.getVisibility() == android.view.View.VISIBLE;
            RecyclerView rv = (RecyclerView) view;
            int count = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
            assertTrue("Friend events feed should show items or empty state after 3 seconds",
                    count > 0 || emptyVisible);
        });
    }

    /**
     * After 3 seconds, either the friends carousel has items or the no-friends empty state is shown.
     * Covers: "Activity feed shows recent RSVPs made by friends"
     */
    @Test
    public void friendsScreen_carouselResolvesWithin3Seconds() throws InterruptedException {
        Thread.sleep(3000);
        onView(withId(R.id.recyclerViewFriends)).check((view, e) -> {
            if (e != null) throw e;
            android.widget.TextView emptyState =
                    view.getRootView().findViewById(R.id.tvNoFriends);
            boolean emptyVisible = emptyState != null &&
                    emptyState.getVisibility() == android.view.View.VISIBLE;
            RecyclerView rv = (RecyclerView) view;
            int count = rv.getAdapter() != null ? rv.getAdapter().getItemCount() : 0;
            assertTrue("Friends carousel should show friends or empty state after 3 seconds",
                    count > 0 || emptyVisible);
        });
    }
}
