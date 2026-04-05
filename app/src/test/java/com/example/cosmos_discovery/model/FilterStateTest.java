package com.example.cosmos_discovery.model;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class FilterStateTest {

    @Test
    public void hasActiveFilters_noFiltersSet_returnsFalse() {
        assertFalse(new FilterState().hasActiveFilters());
    }

    @Test
    public void hasActiveFilters_withCategory_returnsTrue() {
        FilterState s = new FilterState();
        s.setSelectedCategories(Collections.singletonList("music"));
        assertTrue(s.hasActiveFilters());
    }

    @Test
    public void hasActiveFilters_withDate_returnsTrue() {
        FilterState s = new FilterState();
        s.setSelectedDate(FilterState.DateFilter.TODAY);
        assertTrue(s.hasActiveFilters());
    }

    @Test
    public void hasActiveFilters_withAccess_returnsTrue() {
        FilterState s = new FilterState();
        s.setSelectedAccess(FilterState.AccessFilter.LUMS_ONLY);
        assertTrue(s.hasActiveFilters());
    }

    @Test
    public void getDateRange_noDateFilter_returnsNull() {
        assertNull(new FilterState().getDateRange());
    }

    @Test
    public void getDateRange_today_containsCurrentTime() {
        FilterState s = new FilterState();
        s.setSelectedDate(FilterState.DateFilter.TODAY);
        long[] range = s.getDateRange();
        long now = System.currentTimeMillis();
        assertTrue(range[0] <= now);
        assertTrue(range[1] >= now);
    }

    @Test
    public void getDateRange_thisWeek_containsCurrentTime() {
        FilterState s = new FilterState();
        s.setSelectedDate(FilterState.DateFilter.THIS_WEEK);
        long[] range = s.getDateRange();
        long now = System.currentTimeMillis();
        assertTrue(range[0] <= now && now <= range[1]);
    }

    @Test
    public void getDateRange_startAlwaysBeforeEnd() {
        for (FilterState.DateFilter d : FilterState.DateFilter.values()) {
            FilterState s = new FilterState();
            s.setSelectedDate(d);
            long[] range = s.getDateRange();
            assertNotNull("getDateRange() returned null for " + d, range);
            assertTrue("start must be <= end for " + d, range[0] <= range[1]);
        }
    }

    @Test
    public void setSelectedCategories_null_setsEmptyList() {
        FilterState s = new FilterState();
        s.setSelectedCategories(null);
        assertNotNull(s.getSelectedCategories());
        assertTrue(s.getSelectedCategories().isEmpty());
        assertFalse(s.hasActiveFilters());
    }
}
