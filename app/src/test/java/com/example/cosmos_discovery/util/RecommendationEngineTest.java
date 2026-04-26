package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecommendationEngineTest {

    private static final long FUTURE = System.currentTimeMillis() + 86_400_000L; // +1 day
    private static final String UID  = "user123";

    private Event makeEvent(String id, String category, String... tags) {
        Event e = new Event(id, FUTURE, "location", Arrays.asList(tags), null, "org1");
        e.setId(id);
        e.setCategory(category);
        e.setStatus(Event.STATUS_APPROVED);
        return e;
    }

    private Event makePopularEvent(String id, int rsvpCount) {
        Event e = makeEvent(id, "misc");
        e.setRsvpCount(rsvpCount);
        return e;
    }

    // ── Category weighting ────────────────────────────────────────────────

    @Test
    public void categoryMatch_ranksHigherThanTagOnlyMatch() {
        Event history = makeEvent("h1", "music", "live");

        Event catMatch = makeEvent("c1", "music");           // +2 category
        Event tagMatch = makeEvent("t1", "other", "live");   // +1 tag

        List<Event> results = RecommendationEngine.recommend(
                list(history), list(catMatch, tagMatch), empty(), UID);

        assertEquals("category match should rank first", "c1", results.get(0).getId());
        assertEquals("tag match should rank second", "t1", results.get(1).getId());
    }

    // ── Dismissed exclusion ───────────────────────────────────────────────

    @Test
    public void dismissedEvent_isExcluded() {
        Event history     = makeEvent("h1", "music");
        Event dismissed   = makeEvent("d1", "music");
        Event notDismissed = makeEvent("nd1", "music");

        Set<String> dismissed_ids = new HashSet<>(Collections.singletonList("d1"));

        List<Event> results = RecommendationEngine.recommend(
                list(history), list(dismissed, notDismissed), dismissed_ids, UID);

        for (Event e : results) {
            assertFalse("dismissed event should not appear", e.getId().equals("d1"));
        }
        assertTrue("non-dismissed event should appear",
                results.stream().anyMatch(e -> e.getId().equals("nd1")));
    }

    // ── Already-RSVPed exclusion ──────────────────────────────────────────

    @Test
    public void rsvpedEvent_isExcluded() {
        Event history = makeEvent("h1", "music");
        Event rsvped  = makeEvent("r1", "music");
        rsvped.setAttendeeIds(Collections.singletonList(UID));

        Event notRsvped = makeEvent("nr1", "music");

        List<Event> results = RecommendationEngine.recommend(
                list(history), list(rsvped, notRsvped), empty(), UID);

        for (Event e : results) {
            assertFalse("rsvped event should not appear", e.getId().equals("r1"));
        }
    }

    // ── Fallback ──────────────────────────────────────────────────────────

    @Test
    public void noHistory_fallbackReturnsMinimumThree() {
        Event e1 = makePopularEvent("p1", 50);
        Event e2 = makePopularEvent("p2", 30);
        Event e3 = makePopularEvent("p3", 10);

        List<Event> results = RecommendationEngine.recommend(
                Collections.emptyList(), list(e1, e2, e3), empty(), UID);

        assertEquals("fallback should return 3 events", 3, results.size());
    }

    @Test
    public void partialHistory_fallbackPadsToThree() {
        Event history = makeEvent("h1", "rareCategory");
        Event match   = makeEvent("m1", "rareCategory"); // score > 0
        // No other events have this category — only 1 scored result
        Event popular1 = makePopularEvent("pop1", 100);
        Event popular2 = makePopularEvent("pop2", 80);

        List<Event> results = RecommendationEngine.recommend(
                list(history), list(match, popular1, popular2), empty(), UID);

        assertTrue("should have at least 3 results", results.size() >= 3);
        assertEquals("scored match should be first", "m1", results.get(0).getId());
    }

    // ── Past events excluded ──────────────────────────────────────────────

    @Test
    public void pastEvent_isExcluded() {
        Event pastEvent = new Event("past", System.currentTimeMillis() - 1000,
                "loc", Collections.emptyList(), null, "org");
        pastEvent.setId("past");
        pastEvent.setCategory("music");
        pastEvent.setStatus(Event.STATUS_APPROVED);

        Event futureEvent = makeEvent("future", "music");

        Event history = makeEvent("h1", "music");

        List<Event> results = RecommendationEngine.recommend(
                list(history), list(pastEvent, futureEvent), empty(), UID);

        for (Event e : results) {
            assertFalse("past event should not appear", e.getId().equals("past"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    private static Set<String> empty() {
        return Collections.emptySet();
    }
}
