package com.example.cosmos_discovery.database;

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

    public void fetchAllNonAdminUsers(Consumer<List<User>> onSuccess, Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && !User.ROLE_ADMIN.equals(user.getRole())) {
                            users.add(user);
                        }
                    }
                    onSuccess.accept(users);
                })
                .addOnFailureListener(e -> onFailure.accept("Could not load users."));
    }

    public void updateUserRole(String userId, String newRole,
            Runnable onSuccess, Consumer<String> onFailure) {
        if (User.ROLE_ADMIN.equals(newRole)) {
            onFailure.accept("Cannot promote to admin via this interface.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);
        if (User.ROLE_STUDENT.equals(newRole)) {
            updates.put("promotionApproved", false);
        }
        mDb.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not update role."));
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
}
