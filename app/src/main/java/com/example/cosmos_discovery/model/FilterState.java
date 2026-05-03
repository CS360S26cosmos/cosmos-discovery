package com.example.cosmos_discovery.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Holds the active filter selections for the event filter overlay.
 *
 * All fields are nullable/empty — empty/null means "no filter applied" for that group.
 * Categories support multi-select; date and access type are single-select.
 *
 * Pass to {@link com.example.cosmos_discovery.ui.student.SearchFragment#updateFilters(FilterState)}
 * when the user taps "View Results" or "Clear Filters".
 */
public class FilterState {

    /** Predefined date-range options shown as chips in the filter overlay. */
    public enum DateFilter { TODAY, TOMORROW, THIS_WEEK, THIS_WEEKEND, THIS_MONTH }

    /** Event visibility options — who the event is open to. */
    public enum AccessFilter { LUMS_ONLY, OPEN }

    private List<String> selectedCategories = new ArrayList<>(); // empty = no category filter
    private DateFilter   selectedDate;                           // null  = no date filter
    private AccessFilter selectedAccess;                         // null  = no access filter

    // ── Getters / Setters ──────────────────────────────────────────────────

    public List<String> getSelectedCategories()                        { return selectedCategories; }
    public void         setSelectedCategories(List<String> categories) { this.selectedCategories = categories != null ? categories : new ArrayList<>(); }

    public DateFilter getSelectedDate()                    { return selectedDate; }
    public void       setSelectedDate(DateFilter date)     { this.selectedDate = date; }

    public AccessFilter getSelectedAccess()                { return selectedAccess; }
    public void         setSelectedAccess(AccessFilter a)  { this.selectedAccess = a; }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Returns true if any filter criterion is active. */
    public boolean hasActiveFilters() {
        return !selectedCategories.isEmpty() || selectedDate != null || selectedAccess != null;
    }

    /**
     * Returns [startMs, endMs] for the selected date filter, or null if no date filter is set.
     * Uses the same Calendar pattern as EventService.currentWeekBounds().
     */
    public long[] getDateRange() {
        if (selectedDate == null) return null;

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        switch (selectedDate) {
            case TODAY:
                // start/end already set to today 00:00 → 23:59
                break;
            case TOMORROW:
                start.add(Calendar.DAY_OF_YEAR, 1);
                end.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case THIS_WEEK:
                // Use arithmetic add() instead of set(DAY_OF_WEEK) — set() is locale-dependent
                // and can jump forward when today is past the target day, inverting the range.
                int dayOfWeek = start.get(Calendar.DAY_OF_WEEK); // SUNDAY=1 … SATURDAY=7
                start.add(Calendar.DAY_OF_YEAR, -(dayOfWeek - Calendar.SUNDAY)); // roll back to Sunday
                end.add(Calendar.DAY_OF_YEAR, Calendar.SATURDAY - dayOfWeek);    // roll forward to Saturday
                break;
            case THIS_WEEKEND:
                // Use add() arithmetic — set(DAY_OF_WEEK) is locale-dependent (see THIS_WEEK comment)
                int today = start.get(Calendar.DAY_OF_WEEK); // SUNDAY=1 … SATURDAY=7
                int daysUntilSat = (Calendar.SATURDAY - today + 7) % 7;
                if (daysUntilSat == 0) daysUntilSat = 7; // already Saturday → target next Saturday
                start.add(Calendar.DAY_OF_YEAR, daysUntilSat);
                end = (Calendar) start.clone();
                end.add(Calendar.DAY_OF_YEAR, 1); // Sunday after that Saturday
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                break;
            case THIS_MONTH:
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            default:
                return null;
        }
        return new long[]{ start.getTimeInMillis(), end.getTimeInMillis() };
    }
}
