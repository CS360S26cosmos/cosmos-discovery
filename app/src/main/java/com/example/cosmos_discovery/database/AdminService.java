package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.DashboardMetrics;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.OrganizerRequest;
import com.example.cosmos_discovery.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdminService {

    private static final String USERS_COLLECTION    = "users";
    private static final String REQUESTS_COLLECTION = "organizer_requests";
    private static final String EVENTS_COLLECTION   = "events";

    private final FirebaseFirestore mDb;

    public AdminService() {
        this(FirebaseFirestore.getInstance());
    }

    AdminService(FirebaseFirestore db) {
        mDb = db;
    }

    public ListenerRegistration listenPendingRequests(
            Consumer<List<OrganizerRequest>> onUpdate, Consumer<String> onError) {
        return mDb.collection(REQUESTS_COLLECTION)
                .whereEqualTo("status", OrganizerRequest.STATUS_PENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { onError.accept("Could not load requests."); return; }
                    List<OrganizerRequest> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            OrganizerRequest req = doc.toObject(OrganizerRequest.class);
                            if (req != null) list.add(req);
                        }
                    }
                    onUpdate.accept(list);
                });
    }

    public void approveRequest(String userId, Runnable onSuccess, Consumer<String> onFailure) {
        WriteBatch batch = mDb.batch();
        batch.update(mDb.collection(USERS_COLLECTION).document(userId),
                "role", User.ROLE_ORGANIZER,
                "promotionApproved", true);
        batch.delete(mDb.collection(REQUESTS_COLLECTION).document(userId));
        batch.commit()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not approve request."));
    }

    public void rejectRequest(String userId, Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(REQUESTS_COLLECTION).document(userId)
                .delete()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not reject request."));
    }

    public void fetchAllUsers(Consumer<List<User>> onSuccess, Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            users.add(user);
                        }
                    }
                    onSuccess.accept(users);
                })
                .addOnFailureListener(e -> onFailure.accept("Could not load users."));
    }

    public void updateUserRole(String userId, String newRole,
            Runnable onSuccess, Consumer<String> onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);
        if (!User.ROLE_ORGANIZER.equals(newRole)) {
            updates.put("promotionApproved", false);
        }
        mDb.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not update role: " + e.getMessage()));
    }

    public void deactivateUser(String userId, Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION).document(userId)
                .update("active", false)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not deactivate user."));
    }

    public void reactivateUser(String userId, Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION).document(userId)
                .update("active", true)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not reactivate user."));
    }

    /**
     * Fetches all events and users in parallel, then computes dashboard metrics.
     *
     * @param fromMs          start of date range filter (epoch ms, inclusive); 0 = all-time
     * @param toMs            end of date range filter (epoch ms, inclusive); Long.MAX_VALUE = all-time
     * @param onEventsLoaded  called with the full (unfiltered) event list for CSV export caching
     * @param onSuccess       called with computed {@link DashboardMetrics}
     * @param onError         called with an error message if either Firestore call fails
     */
    public void fetchDashboardMetrics(long fromMs, long toMs,
            Consumer<List<Event>> onEventsLoaded,
            Consumer<DashboardMetrics> onSuccess,
            Consumer<String> onError) {

        // Simple int-array latch — both callbacks run on the main thread, so no sync needed.
        int[] remaining = {2};
        int[] userCount = {0};
        @SuppressWarnings("unchecked")
        List<Event>[] eventsHolder = new List[1];

        mDb.collection(EVENTS_COLLECTION).get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e == null) continue;
                        e.setId(doc.getId());
                        list.add(e);
                    }
                    eventsHolder[0] = list;
                    if (--remaining[0] == 0) {
                        onEventsLoaded.accept(eventsHolder[0]);
                        onSuccess.accept(
                                DashboardMetrics.compute(eventsHolder[0], userCount[0], fromMs, toMs));
                    }
                })
                .addOnFailureListener(e -> onError.accept("Could not load events."));

        mDb.collection(USERS_COLLECTION).get()
                .addOnSuccessListener(snapshot -> {
                    userCount[0] = snapshot.size();
                    if (--remaining[0] == 0) {
                        onEventsLoaded.accept(eventsHolder[0]);
                        onSuccess.accept(
                                DashboardMetrics.compute(eventsHolder[0], userCount[0], fromMs, toMs));
                    }
                })
                .addOnFailureListener(e -> onError.accept("Could not load users."));
    }
}
