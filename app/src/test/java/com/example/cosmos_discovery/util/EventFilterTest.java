package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FilterState;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EventFilterTest {

    private Event event(String title, String location, List<String> tags) {
        Event e = new Event(title, 0L, location, tags, null, null);
        return e;
    }

    // ── textSearch ──────────────────────────────────────────────────────────

    @Test
    public void textSearch_nullQuery_returnsAll() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(1, EventFilter.textSearch(events, null).size());
    }

    @Test
    public void textSearch_emptyQuery_returnsAll() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(1, EventFilter.textSearch(events, "").size());
    }

    @Test
    public void textSearch_matchesTitle() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(1, EventFilter.textSearch(events, "hackathon").size());
    }

    @Test
    public void textSearch_matchesLocation() {
        List<Event> events = Collections.singletonList(event("Seminar", "Auditorium", null));
        assertEquals(1, EventFilter.textSearch(events, "auditorium").size());
    }

    @Test
    public void textSearch_matchesTags() {
        Event e = event("Event", "Place", Arrays.asList("music", "outdoor"));
        assertEquals(1, EventFilter.textSearch(Collections.singletonList(e), "music").size());
    }

    @Test
    public void textSearch_multipleTokens_allMustMatch() {
        Event e = event("Music Festival", "Outdoor Stage", null);
        List<Event> single = Collections.singletonList(e);
        assertEquals(1, EventFilter.textSearch(single, "music outdoor").size());
        assertEquals(0, EventFilter.textSearch(single, "music tech").size());
    }

    @Test
    public void textSearch_noMatch_returnsEmpty() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(0, EventFilter.textSearch(events, "concert").size());
    }

    @Test
    public void textSearch_caseInsensitive() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(1, EventFilter.textSearch(events, "HACKATHON").size());
    }

    // ── applyFilters ────────────────────────────────────────────────────────

    @Test
    public void applyFilters_nullState_returnsAll() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(1, EventFilter.applyFilters(new ArrayList<>(events), null).size());
    }

    @Test
    public void applyFilters_noActiveFilters_returnsAll() {
        List<Event> events = Collections.singletonList(event("Hackathon", "LUMS", null));
        assertEquals(1, EventFilter.applyFilters(new ArrayList<>(events), new FilterState()).size());
    }

    @Test
    public void applyFilters_categoryMatch_includesEvent() {
        Event e = event("Music Fest", "Stage", Arrays.asList("music"));
        FilterState state = new FilterState();
        state.setSelectedCategories(Collections.singletonList("music"));
        assertEquals(1, EventFilter.applyFilters(new ArrayList<>(Collections.singletonList(e)), state).size());
    }

    @Test
    public void applyFilters_categoryNoMatch_excludesEvent() {
        Event e = event("Tech Talk", "Lab", Arrays.asList("tech"));
        FilterState state = new FilterState();
        state.setSelectedCategories(Collections.singletonList("sports"));
        assertEquals(0, EventFilter.applyFilters(new ArrayList<>(Collections.singletonList(e)), state).size());
    }

    @Test
    public void applyFilters_accessLumsOnly_includesMatchingEvent() {
        Event e = event("LUMS Event", "LUMS", null);
        e.setAccessType("lums_only");
        FilterState state = new FilterState();
        state.setSelectedAccess(FilterState.AccessFilter.LUMS_ONLY);
        assertEquals(1, EventFilter.applyFilters(new ArrayList<>(Collections.singletonList(e)), state).size());
    }

    @Test
    public void applyFilters_accessOpen_excludesLumsEvent() {
        Event e = event("LUMS Event", "LUMS", null);
        e.setAccessType("lums_only");
        FilterState state = new FilterState();
        state.setSelectedAccess(FilterState.AccessFilter.OPEN);
        assertEquals(0, EventFilter.applyFilters(new ArrayList<>(Collections.singletonList(e)), state).size());
    }

    @Test
    public void applyFilters_dateRange_excludesEventOutsideRange() {
        Event e = event("Old Event", "LUMS", null);
        e.setDateTime(1_000L); // epoch ms far in the past
        FilterState state = new FilterState();
        state.setSelectedDate(FilterState.DateFilter.TODAY);
        assertEquals(0, EventFilter.applyFilters(new ArrayList<>(Collections.singletonList(e)), state).size());
    }

    @Test
    public void applyFilters_dateRange_includesEventInsideRange() {
        Event e = event("Today Event", "LUMS", null);
        e.setDateTime(System.currentTimeMillis());
        FilterState state = new FilterState();
        state.setSelectedDate(FilterState.DateFilter.TODAY);
        assertEquals(1, EventFilter.applyFilters(new ArrayList<>(Collections.singletonList(e)), state).size());
    }
}
