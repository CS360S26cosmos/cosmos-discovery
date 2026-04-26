package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.model.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side recommendation engine. Pure Java — no Android or Firebase dependencies.
 *
 * Scoring:
 *   +2 for each category match against the user's RSVP history
 *   +1 for each tag match against the user's RSVP history
 *
 * Events already RSVPed by the user, in the dismissed set, or in the past are excluded.
 * If fewer than 3 events have a positive score, popular events (by rsvpCount) are added
 * as a fallback until the minimum of 3 is reached.
 */
public class RecommendationEngine {

    private static final int MAX_RESULTS     = 10;
    private static final int MIN_RESULTS     = 3;
    private static final int SCORE_CATEGORY  = 2;
    private static final int SCORE_TAG       = 1;

    private RecommendationEngine() {}

    /**
     * @param rsvpHistory  events the current user has previously RSVPed to
     * @param candidates   all upcoming approved events
     * @param dismissedIds event IDs the user has dismissed as "Not Interested"
     * @param currentUid   UID of the current user (to exclude already-RSVPed events)
     * @return ranked list of recommended events, capped at {@value MAX_RESULTS}
     */
    public static List<Event> recommend(List<Event> rsvpHistory,
                                        List<Event> candidates,
                                        Set<String> dismissedIds,
                                        String currentUid) {
        long now = System.currentTimeMillis();

        Map<String, Integer> categoryFreq = buildCategoryFreq(rsvpHistory);
        Map<String, Integer> tagFreq      = buildTagFreq(rsvpHistory);

        List<ScoredEvent> scored = new ArrayList<>();
        for (Event event : candidates) {
            if (shouldExclude(event, dismissedIds, currentUid, now)) continue;
            int score = scoreEvent(event, categoryFreq, tagFreq);
            scored.add(new ScoredEvent(event, score));
        }

        Collections.sort(scored, (a, b) -> Integer.compare(b.score, a.score));

        List<Event> results = new ArrayList<>();
        for (ScoredEvent se : scored) {
            if (results.size() >= MAX_RESULTS) break;
            if (se.score > 0) results.add(se.event);
        }

        // Fallback: pad with most-popular events not already in results
        if (results.size() < MIN_RESULTS) {
            List<Event> popular = new ArrayList<>();
            for (ScoredEvent se : scored) {
                if (!results.contains(se.event)) popular.add(se.event);
            }
            Collections.sort(popular, (a, b) -> Integer.compare(b.getRsvpCount(), a.getRsvpCount()));
            for (Event e : popular) {
                if (results.size() >= MIN_RESULTS) break;
                results.add(e);
            }
        }

        return results;
    }

    private static boolean shouldExclude(Event event, Set<String> dismissedIds,
                                         String uid, long now) {
        if (event.getId() == null) return true;
        if (event.getDateTime() <= now) return true;
        if (dismissedIds.contains(event.getId())) return true;
        if (event.isRsvped(uid)) return true;
        return false;
    }

    private static int scoreEvent(Event event,
                                  Map<String, Integer> categoryFreq,
                                  Map<String, Integer> tagFreq) {
        int score = 0;
        String category = event.getCategory();
        if (category != null && categoryFreq.containsKey(category)) {
            score += SCORE_CATEGORY * categoryFreq.get(category);
        }
        if (event.getTags() != null) {
            for (String tag : event.getTags()) {
                if (tagFreq.containsKey(tag)) {
                    score += SCORE_TAG * tagFreq.get(tag);
                }
            }
        }
        return score;
    }

    private static Map<String, Integer> buildCategoryFreq(List<Event> history) {
        Map<String, Integer> freq = new HashMap<>();
        for (Event e : history) {
            if (e.getCategory() != null) {
                freq.merge(e.getCategory(), 1, Integer::sum);
            }
        }
        return freq;
    }

    private static Map<String, Integer> buildTagFreq(List<Event> history) {
        Map<String, Integer> freq = new HashMap<>();
        for (Event e : history) {
            if (e.getTags() != null) {
                for (String tag : e.getTags()) {
                    freq.merge(tag, 1, Integer::sum);
                }
            }
        }
        return freq;
    }

    private static class ScoredEvent {
        final Event event;
        final int   score;
        ScoredEvent(Event event, int score) {
            this.event = event;
            this.score = score;
        }
    }
}
