package com.example.cosmos_discovery.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Persists event IDs that the current user has dismissed as "Not Interested". */
public class DismissedEventsStore {

    private static final String PREFS_NAME = "dismissed_events";
    private static final String KEY_IDS    = "ids";

    private final SharedPreferences mPrefs;

    public DismissedEventsStore(Context context) {
        mPrefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void dismiss(String eventId) {
        Set<String> copy = new HashSet<>(getDismissedIds());
        copy.add(eventId);
        mPrefs.edit().putStringSet(KEY_IDS, copy).apply();
    }

    /**
     * Returns a defensive copy of the dismissed-ID set.
     * SharedPreferences StringSet must never be modified directly — Android may
     * return a reference to its internal instance, mutating which corrupts the cache.
     */
    public Set<String> getDismissedIds() {
        Set<String> stored = mPrefs.getStringSet(KEY_IDS, null);
        return stored != null ? new HashSet<>(stored) : Collections.emptySet();
    }

    public void clear() {
        mPrefs.edit().remove(KEY_IDS).apply();
    }
}
