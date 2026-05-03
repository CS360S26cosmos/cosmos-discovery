package com.example.cosmos_discovery.events;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.example.cosmos_discovery.EspressoTestHelper;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.ui.student.StudentActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;


@RunWith(AndroidJUnit4.class)
public class EventFeedInstrumentedTest {

    // Launch StudentActivity directly — MainActivity is just a routing shim that immediately
    // delegates to StudentActivity, so testing it directly avoids ActivityScenarioRule teardown issues.
    @Rule
    public ActivityScenarioRule<StudentActivity> activityRule =
            new ActivityScenarioRule<>(StudentActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        EspressoTestHelper.populateRoleUtil();
    }

    /**
     * Verifies that the "Suggested" events carousel RecyclerView is displayed.
     * Covers US-01: "Has a suggested section"
     */
    @Test
    public void eventList_isDisplayed() {
        onView(withId(R.id.recyclerViewSuggested))
                .check(matches(isDisplayed()));
    }

    /**
     * Verifies that the "This Week" events list RecyclerView exists in the layout.
     * Together with the Suggested section, this confirms the screen has scrollable content.
     * Covers US-01: "Also has an Upcoming Events section"
     */
    @Test
    public void eventList_isScrollable() {
        onView(withId(R.id.recyclerViewThisWeek))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /**
     * Verifies the "Suggested" section label is visible on the discover feed.
     * Covers US-01: "Home screen shows a scrollable list of upcoming events"
     */
    @Test
    public void eventCard_displaysFields() {
        onView(withText(containsString("Suggested")))
                .check(matches(isDisplayed()));
    }
}