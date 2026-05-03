package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.Notification;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Firestore-backed access layer for the per-user notification feed.
 *
 * <p>Each user has a {@code users/{uid}/notifications} subcollection containing
 * one document per notification. This service exposes async write/delete/listen
 * operations and a backfill helper for the {@code audience} field added in a
 * later schema version.
 *
 * <p>Async methods take {@code onSuccess} / {@code onError} callbacks rather
 * than returning values. Real-time listeners return a
 * {@link ListenerRegistration} which the caller MUST remove in
 * {@code onDestroyView()} or {@code onStop()} to avoid leaks.
 */
public class NotificationService {

    private static final String USERS_COL  = "users";
    private static final String NOTIFS_SUB = "notifications";

    private final FirebaseFirestore mDb = FirebaseFirestore.getInstance();

    // ── Write ─────────────────────────────────────────────────────────────

    /** Writes a single notification document to one user's subcollection. */
    public void writeNotification(String userId, Notification notif,
                                  Runnable onSuccess, Consumer<String> onError) {
        mDb.collection(USERS_COL)
                .document(userId)
                .collection(NOTIFS_SUB)
                .add(toMap(notif))
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e  -> onError.accept("Failed to write notification."));
    }

    /**
     * Writes the same notification to multiple users atomically via WriteBatch.
     * Safe for lists up to 500 users (Firestore batch limit).
     */
    public void writeNotificationToUsers(List<String> userIds, Notification notif,
                                          Runnable onSuccess, Consumer<String> onError) {
        writeNotificationToUsers(userIds, null, notif, onSuccess, onError);
    }

    /**
     * Same as above but uses a fixed {@code docId} for each user's notification document,
     * so repeated calls overwrite the previous notification instead of appending a new one.
     * Pass {@code null} for {@code docId} to use an auto-generated ID.
     */
    public void writeNotificationToUsers(List<String> userIds, String docId, Notification notif,
                                          Runnable onSuccess, Consumer<String> onError) {
        if (userIds == null || userIds.isEmpty()) {
            onSuccess.run();
            return;
        }
        WriteBatch batch = mDb.batch();
        Map<String, Object> data = toMap(notif);
        for (String uid : userIds) {
            DocumentReference ref = docId != null
                    ? mDb.collection(USERS_COL).document(uid).collection(NOTIFS_SUB).document(docId)
                    : mDb.collection(USERS_COL).document(uid).collection(NOTIFS_SUB).document();
            batch.set(ref, data);
        }
        batch.commit()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e      -> onError.accept("Failed to notify attendees."));
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /** Deletes a single notification document from a user's subcollection. */
    public void deleteNotification(String userId, String notifId,
                                   Runnable onSuccess, Consumer<String> onError) {
        mDb.collection(USERS_COL)
                .document(userId)
                .collection(NOTIFS_SUB)
                .document(notifId)
                .delete()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onError.accept("Failed to delete notification."));
    }

    /**
     * Batch-deletes all supplied notification documents for the given user.
     * Uses WriteBatch so all deletes are atomic (safe for up to 500 items).
     */
    public void deleteAllNotifications(String userId, List<Notification> notifications,
                                       Runnable onSuccess, Consumer<String> onError) {
        if (notifications == null || notifications.isEmpty()) {
            onSuccess.run();
            return;
        }
        WriteBatch batch = mDb.batch();
        for (Notification n : notifications) {
            if (n.getId() == null) continue;
            DocumentReference ref = mDb.collection(USERS_COL)
                    .document(userId)
                    .collection(NOTIFS_SUB)
                    .document(n.getId());
            batch.delete(ref);
        }
        batch.commit()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onError.accept("Failed to clear notifications."));
    }

    // ── Listen ────────────────────────────────────────────────────────────

    /**
     * Returns a real-time listener on the user's notifications subcollection,
     * ordered newest-first. Caller must call remove() in onDestroyView().
     */
    public ListenerRegistration listenNotifications(String userId,
                                                     Consumer<List<Notification>> onUpdate,
                                                     Consumer<String> onError) {
        return mDb.collection(USERS_COL)
                .document(userId)
                .collection(NOTIFS_SUB)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load notifications.");
                        return;
                    }
                    List<Notification> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            list.add(n);
                        }
                    }
                    onUpdate.accept(list);
                });
    }

    /**
     * Stamps the {@code audience} field on any legacy notification docs missing it.
     * Inferred from the notification type. Safe to call repeatedly — only writes
     * docs that lack the field.
     */
    public void backfillAudience(String userId, List<Notification> notifications) {
        if (userId == null || notifications == null || notifications.isEmpty()) return;

        WriteBatch batch = mDb.batch();
        int writes = 0;
        for (Notification n : notifications) {
            if (n.getId() == null || n.getAudience() != null) continue;
            String audience = inferAudience(n.getType());
            DocumentReference ref = mDb.collection(USERS_COL)
                    .document(userId)
                    .collection(NOTIFS_SUB)
                    .document(n.getId());
            batch.update(ref, "audience", audience);
            writes++;
            if (writes >= 400) break; // Stay well under Firestore's 500-op batch limit.
        }
        if (writes > 0) batch.commit();
    }

    private String inferAudience(String type) {
        if (type == null) return Notification.AUDIENCE_PERSONAL;
        switch (type) {
            case Notification.TYPE_RSVP_RECEIVED:
            case Notification.TYPE_CAPACITY_FULL:
            case Notification.TYPE_EVENT_APPROVED:
            case Notification.TYPE_EVENT_REJECTED:
                return Notification.AUDIENCE_ORGANIZER;
            default:
                return Notification.AUDIENCE_PERSONAL;
        }
    }

    // ── Private ───────────────────────────────────────────────────────────

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new HashMap<>();
        m.put("type",      n.getType());
        m.put("title",     n.getTitle());
        m.put("message",   n.getMessage());
        m.put("timestamp", n.getTimestamp());
        m.put("read",      false);
        if (n.getEventId()    != null) m.put("eventId",    n.getEventId());
        if (n.getEventTitle() != null) m.put("eventTitle", n.getEventTitle());
        if (n.getAudience()   != null) m.put("audience",   n.getAudience());
        return m;
    }
}
