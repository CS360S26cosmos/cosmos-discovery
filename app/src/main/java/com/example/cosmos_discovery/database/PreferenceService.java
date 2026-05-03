package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reads and writes the {@code notifPrefs} map on a user document.
 * Missing prefs are treated as enabled (default-true).
 */
public class PreferenceService {

    private static final String USERS_COL = "users";
    private static final String FIELD     = "notifPrefs";

    private final FirebaseFirestore mDb = FirebaseFirestore.getInstance();

    /** Loads the user's prefs map; returns all-true defaults if the doc is missing the field. */
    public void getPrefs(String uid,
                         Consumer<Map<String, Boolean>> onSuccess,
                         Consumer<String> onError) {
        mDb.collection(USERS_COL)
                .document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, Boolean> defaults = User.defaultNotifPrefs();
                    Object raw = snap.get(FIELD);
                    if (raw instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> stored = (Map<String, Object>) raw;
                        for (Map.Entry<String, Object> e : stored.entrySet()) {
                            if (e.getValue() instanceof Boolean) {
                                defaults.put(e.getKey(), (Boolean) e.getValue());
                            }
                        }
                    }
                    onSuccess.accept(defaults);
                })
                .addOnFailureListener(e -> onError.accept("Could not load notification preferences."));
    }

    /**
     * Fills in any missing preference keys on the user doc with their default-true values,
     * leaving existing keys (including ones the user has explicitly turned off) untouched.
     * Safe to call on every login — it's a no-op once the doc is fully populated.
     */
    public void ensurePrefsInitialized(String uid) {
        if (uid == null || uid.isEmpty()) return;
        mDb.collection(USERS_COL).document(uid).get()
                .addOnSuccessListener(snap -> {
                    Map<String, Boolean> defaults = User.defaultNotifPrefs();
                    Object raw = snap.get(FIELD);

                    Map<String, Object> missing = new HashMap<>();
                    if (!(raw instanceof Map)) {
                        // Doc has no prefs map at all — write the full defaults under "notifPrefs".
                        for (Map.Entry<String, Boolean> e : defaults.entrySet()) {
                            missing.put(FIELD + "." + e.getKey(), e.getValue());
                        }
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> stored = (Map<String, Object>) raw;
                        for (Map.Entry<String, Boolean> e : defaults.entrySet()) {
                            if (!stored.containsKey(e.getKey())) {
                                missing.put(FIELD + "." + e.getKey(), e.getValue());
                            }
                        }
                    }
                    if (missing.isEmpty()) return;
                    mDb.collection(USERS_COL).document(uid).update(missing);
                });
    }

    /** Updates a single preference key. Use dot-notation field path so other keys aren't clobbered. */
    public void updatePref(String uid, String key, boolean value,
                           Runnable onSuccess, Consumer<String> onError) {
        mDb.collection(USERS_COL)
                .document(uid)
                .update(FIELD + "." + key, value)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> {
                    // Legacy doc with no notifPrefs map yet — write the whole map with this override.
                    Map<String, Boolean> prefs = User.defaultNotifPrefs();
                    prefs.put(key, value);
                    Map<String, Object> wrap = new HashMap<>();
                    wrap.put(FIELD, prefs);
                    mDb.collection(USERS_COL).document(uid)
                            .set(wrap, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(unused -> onSuccess.run())
                            .addOnFailureListener(ex -> onError.accept("Could not save preference."));
                });
    }
}
