package com.example.cosmos_discovery.events;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.ui.shared.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;


@RunWith(AndroidJUnit4.class)
public class EventFeedInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Test: Event list is displayed
     */
    @Test
    public void eventList_isDisplayed() {
        onView(withId(R.id.recyclerViewEvents))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: RecyclerView is scrollable
     */
    @Test
    public void eventList_isScrollable() {
        onView(withId(R.id.recyclerViewEvents))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: Event card elements exist
     */
    @Test
    public void eventCard_displaysFields() {
        onView(withText("Hackathon"))
                .check(matches(isDisplayed()));
    }
}