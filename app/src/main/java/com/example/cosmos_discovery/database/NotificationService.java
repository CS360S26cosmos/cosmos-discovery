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
        return m;
    }
}
