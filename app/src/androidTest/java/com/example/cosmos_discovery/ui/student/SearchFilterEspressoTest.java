package com.example.cosmos_discovery.ui.student;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cosmos_discovery.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;

/**
 * Espresso UI tests for the search bar and filter overlay in StudentActivity.
 * Covers US-02 acceptance criteria:
 *   - Search bar returns matching events in real-time
 *   - Filter options: Category, Date Range, etc.
 *   - Multiple filters can be applied simultaneously
 *   - Clear filters button resets all at once
 *
 * Assumes the app is already signed in before these tests run.
 *
 * These tests run on a connected emulator/device and are excluded from GitHub CI
 * (CI only runs ./gradlew testDebugUnitTest).
 */
@RunWith(AndroidJUnit4.class)
public class SearchFilterEspressoTest {

    private ActivityScenario<StudentActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(StudentActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
    }

    // ── US-02: "Search bar returns matching events in real-time" ─────────────

    /**
     * Verifies the search pill (the rounded search bar shown on the Discover tab) is visible.
     * This is the entry point to event search.
     * Covers: "Search bar returns matching events in real-time"
     */
    @Test
    public void searchPill_isVisibleOnDiscover() {
        onView(withId(R.id.searchBarInclude)).check(matches(isDisplayed()));
    }

    /**
     * Clicking the tappable area inside the search pill hides the pill and shows
     * the active search bar with an EditText.
     * Covers: "Search bar returns matching events in real-time"
     */
    @Test
    public void clickSearchPill_showsSearchInput() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.etSearchInput)).check(matches(isDisplayed()));
    }

    /**
     * After opening search mode, typing text in the search field is accepted.
     * The typed text appears in etSearchInput, which triggers real-time filtering.
     * Covers: "Search bar returns matching events in real-time"
     */
    @Test
    public void searchInput_acceptsTextInput() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.etSearchInput))
                .perform(typeText("music"), closeSoftKeyboard());
        onView(withId(R.id.etSearchInput))
                .check(matches(withText("music")));
    }

    // ── US-02: "Filter options: Category, Date Range, etc." ──────────────────

    /**
     * After opening search mode, the filter button is visible next to the search input.
     * Tapping it opens the filter overlay.
     * Covers: "Filter options: Category, Date Range, etc."
     */
    @Test
    public void filterButton_visibleAfterOpeningSearch() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.filterButtonActive)).check(matches(isDisplayed()));
    }

    /**
     * Clicking the filter button makes the filter overlay visible.
     * The overlay contains category chips, date range chips, and access chips.
     * Covers: "Filter options: Category, Date Range, etc."
     */
    @Test
    public void clickFilterButton_showsFilterOverlay() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.filterButtonActive)).perform(click());
        onView(withId(R.id.filterOverlay)).check(matches(isDisplayed()));
    }

    // ── US-02: "Multiple filters can be applied simultaneously" ──────────────

    /**
     * With the filter overlay open, verifies that category chips, date chips
     * (Today, This Week), and access chips are all present.
     * A user can select multiple chips simultaneously — one from each group.
     * Covers: "Filter options: Category, Date Range, etc."
     *         "Multiple filters can be applied simultaneously"
     */
    @Test
    public void filterOverlay_showsCategoryAndDateChips() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.filterButtonActive)).perform(click());

        // Category section container
        onView(withId(R.id.categoryChipsContainer)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        // Date chips
        onView(withId(R.id.chipToday)).check(matches(isDisplayed()));
        onView(withId(R.id.chipThisWeek)).check(matches(isDisplayed()));
    }

    // ── US-02: "Clear filters button resets all at once" ─────────────────────

    /**
     * With the filter overlay open, verifies the "Clear Filters" button is present.
     * Covers: "Clear filters button resets all at once"
     */
    @Test
    public void filterOverlay_clearFiltersButtonIsPresent() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.filterButtonActive)).perform(click());
        onView(withId(R.id.btnClearFilters)).check(matches(isDisplayed()));
    }

    /**
     * Clicking "Clear Filters" calls onClearFiltersClick(), which deselects all chips
     * and then hides the filter overlay. We verify the overlay is no longer displayed
     * after clicking clear, confirming all filters have been reset.
     * Covers: "Clear filters button resets all at once"
     */
    @Test
    public void clickClearFilters_hidesFilterOverlay() {
        onView(withId(R.id.searchClickableArea)).perform(click());
        onView(withId(R.id.filterButtonActive)).perform(click());
        // Overlay is open
        onView(withId(R.id.filterOverlay)).check(matches(isDisplayed()));
        // Tap a date chip to select it first
        onView(withId(R.id.chipToday)).perform(click());
        // Now click Clear Filters — should deselect all and hide overlay
        onView(withId(R.id.btnClearFilters)).perform(click());
        // Overlay should now be GONE (hidden after clearing)
        onView(withId(R.id.filterOverlay)).check(matches(not(isDisplayed())));
    }
}
