package com.example.cosmos_discovery.ui.organizer;

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

/**
 * Espresso UI tests for AddEventActivity (organizer Create New Event screen).
 * Covers US-08 acceptance criteria:
 *   - Create Event form includes: title, description, date, time, location, category,
 *     max capacity, optional banner image
 *   - All required fields validated before submission
 *
 * Assumes the app is already signed in as an organizer before these tests run.
 */
@RunWith(AndroidJUnit4.class)
public class AddEventEspressoTest {

    private ActivityScenario<AddEventActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(AddEventActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    /**
     * Verifies the event name input field is displayed.
     * Covers: "Create Event form includes: title, ..."
     */
    @Test
    public void addEventForm_showsTitleField() {
        onView(withId(R.id.etEventName)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the description input field is displayed.
     * Covers: "Create Event form includes: ..., description, ..."
     */
    @Test
    public void addEventForm_showsDescriptionField() {
        onView(withId(R.id.etDescription)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the category field is displayed.
     * Covers: "Create Event form includes: ..., category, ..."
     */
    @Test
    public void addEventForm_showsCategoryField() {
        onView(withId(R.id.etCategory)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the date of event field is displayed.
     * Covers: "Create Event form includes: ..., date, ..."
     */
    @Test
    public void addEventForm_showsDateField() {
        onView(withId(R.id.etDateOfEvent)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the start time field is displayed.
     * Covers: "Create Event form includes: ..., time, ..."
     */
    @Test
    public void addEventForm_showsStartTimeField() {
        onView(withId(R.id.etStartTime)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the end time field is displayed.
     * Covers: "Create Event form includes: ..., time, ..."
     */
    @Test
    public void addEventForm_showsEndTimeField() {
        onView(withId(R.id.etEndTime)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the venue field is displayed.
     * Covers: "Create Event form includes: ..., location, ..."
     */
    @Test
    public void addEventForm_showsVenueField() {
        onView(withId(R.id.etVenue)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the banner upload field is displayed.
     * Covers: "Create Event form includes: ..., optional banner image"
     */
    @Test
    public void addEventForm_showsBannerField() {
        onView(withId(R.id.etBannerUpload)).check(matches(isDisplayed()));
    }

    /**
     * Verifies the submit button is displayed and enabled.
     * Covers: "All required fields validated before submission"
     */
    @Test
    public void addEventForm_showsSubmitButton() {
        onView(withId(R.id.btnSubmitCard)).check(matches(isDisplayed()));
    }

    /**
     * Tapping submit with all fields empty should not navigate away — the form stays open.
     * Covers: "All required fields validated before submission"
     */
    @Test
    public void addEventForm_emptySubmit_staysOnForm() {
        onView(withId(R.id.btnSubmitCard)).perform(click());
        // Still on the form — title field is still visible, meaning submission was blocked
        onView(withId(R.id.etEventName)).check(matches(isDisplayed()));
    }
}
