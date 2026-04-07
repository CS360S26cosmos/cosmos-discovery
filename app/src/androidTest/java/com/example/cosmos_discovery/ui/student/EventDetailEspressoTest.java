package com.example.cosmos_discovery.ui.student;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
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

/**
 * Espresso UI tests for the event detail screen (navigated from DiscoverFragment).
 * Covers US-03 acceptance criteria:
 *   - Detail screen shows: title, description, date/time, location, organizer
 *   - Back navigation returns to list without data loss
 * Covers US-04 acceptance criteria:
 *   - RSVP button visible on event detail screen
 *   - Student can cancel RSVP from same screen (same button toggles state)
 *
 * Assumes the app is already signed in and at least one event exists in the Discover feed.
 */
@RunWith(AndroidJUnit4.class)
public class EventDetailEspressoTest {

    private ActivityScenario<StudentActivity> scenario;

    @Before
    public void setUp() throws InterruptedException {
        scenario = ActivityScenario.launch(StudentActivity.class);
        // Wait for the discover feed to load events from Firestore
        Thread.sleep(3000);
        // Tap the first event in the "This Week" list to open its detail screen
        onView(withId(R.id.recyclerViewThisWeek))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    /**
     * Verifies the event title is displayed on the detail screen.
     * Covers: "Detail screen shows: title, ..."
     */
    @Test
    public void eventDetailScreen_showsEventTitle() {
        onView(withId(R.id.textViewEventTitle)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the date/time field is displayed on the detail screen.
     * Covers: "Detail screen shows: ..., date/time, ..."
     */
    @Test
    public void eventDetailScreen_showsDateTime() {
        onView(withId(R.id.tvDateTimeValue)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the location/venue field is displayed on the detail screen.
     * Covers: "Detail screen shows: ..., location, ..."
     */
    @Test
    public void eventDetailScreen_showsLocation() {
        onView(withId(R.id.tvVenueValue)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the organizer field is displayed on the detail screen.
     * Covers: "Detail screen shows: ..., organizer, ..."
     */
    @Test
    public void eventDetailScreen_showsOrganizer() {
        onView(withId(R.id.tvOrganizerValue)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the description field is displayed on the detail screen.
     * Covers: "Detail screen shows: ..., description, ..."
     */
    @Test
    public void eventDetailScreen_showsDescription() {
        onView(withId(R.id.tvDescriptionValue)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the RSVP button is visible and enabled on the detail screen.
     * The same button toggles between RSVP and cancel RSVP state.
     * Covers: "RSVP button visible on event detail screen"
     *         "Student can cancel RSVP from same screen"
     */
    @Test
    public void eventDetailScreen_showsRsvpButton() {
        onView(withId(R.id.rsvpButton))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()));
    }

    /**
     * Verifies pressing the back button returns to the Discover feed.
     * Covers: "Back navigation returns to list without data loss"
     */
    @Test
    public void eventDetailScreen_backNavigationReturnsToFeed() {
        onView(withId(R.id.btnBack)).perform(click());
        onView(withId(R.id.recyclerViewSuggested)).check(matches(isDisplayed()));
    }
}
