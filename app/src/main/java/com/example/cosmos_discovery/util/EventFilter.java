package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FilterState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Pure-Java utility for client-side event filtering.
 * Extracted from SearchFragment so logic can be unit-tested without Android dependencies.
 */
public class EventFilter {

    /**
     * All-tokens-must-appear fuzzy search across title, location, and tags.
     * Returns a new list — does not mutate the input.
     */
    public static List<Event> textSearch(List<Event> events, String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>(events);
        String[] tokens = query.toLowerCase(Locale.getDefault()).trim().split("\\s+");
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            List<String> tags = event.getTags();
            String tagText    = (tags != null) ? String.join(" ", tags) : "";
            String searchable = (event.getTitle()    + " "
                               + event.getLocation() + " "
                               + tagText).toLowerCase(Locale.getDefault());
            boolean allMatch = true;
            for (String token : tokens) {
                if (!searchable.contains(token)) { allMatch = false; break; }
            }
            if (allMatch) result.add(event);
        }
        return result;
    }

    /**
     * Applies category, date-range, and access-type filters from a FilterState.
     * Returns a new list — does not mutate the input list.
     */
    public static List<Event> applyFilters(List<Event> events, FilterState state) {
        if (state == null || !state.hasActiveFilters()) return new ArrayList<>(events);
        List<Event> result = new ArrayList<>(events);

        // Category — keep events whose tags contain at least one selected category
        List<String> selectedCategories = state.getSelectedCategories();
        if (!selectedCategories.isEmpty()) {
            result.removeIf(e -> {
                List<String> tags = e.getTags();
                return tags == null || Collections.disjoint(tags, selectedCategories);
            });
        }

        // Date range
        long[] range = state.getDateRange();
        if (range != null) {
            result.removeIf(e -> e.getDateTime() < range[0] || e.getDateTime() > range[1]);
        }

        // Access type
        FilterState.AccessFilter access = state.getSelectedAccess();
        if (access == FilterState.AccessFilter.LUMS_ONLY) {
            result.removeIf(e -> !"lums_only".equals(e.getAccessType()));
        } else if (access == FilterState.AccessFilter.OPEN) {
            result.removeIf(e -> e.getAccessType() != null && !"open".equals(e.getAccessType()));
        }

        return result;
    }
}
