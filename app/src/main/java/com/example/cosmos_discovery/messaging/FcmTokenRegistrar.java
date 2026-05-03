package com.example.cosmos_discovery.messaging;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Fetches the current FCM device token at app start and writes it to the user's doc.
 * Pair with {@link CosmosFcmService#onNewToken(String)} which handles refreshes thereafter.
 */
public final class FcmTokenRegistrar {

    private static final String TAG = "FcmTokenRegistrar";

    private FcmTokenRegistrar() {}

    public static void register(String uid) {
        if (uid == null || uid.isEmpty()) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) return;
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update("fcmToken", token)
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to write token", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to fetch FCM token", e));
    }
}
