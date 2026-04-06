package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.FriendEntry;
import com.example.cosmos_discovery.model.User;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides friendship-related Firestore operations.
 *
 * Friendships are stored as a {@code friends} subcollection under each user document.
 * Adding a friend writes both sides atomically via a {@link WriteBatch}.
 */
public class FriendService {

    private static final String USERS_COLLECTION = "users";
    private static final String FRIENDS_SUB       = "friends";

    private final FirebaseFirestore mDb;

    public FriendService() {
        mDb = FirebaseFirestore.getInstance();
    }

    /**
     * Writes a mutual friendship atomically: adds {@code target} to {@code currentUser}'s
     * friends list and {@code currentUser} to {@code target}'s friends list.
     *
     * @param currentUser The signed-in user initiating the add.
     * @param target      The user being added as a friend.
     * @param onSuccess   Called when both writes commit successfully.
     * @param onFailure   Called with an error message if the batch fails.
     */
    public void addFriend(User currentUser, User target,
                          Runnable onSuccess, Consumer<String> onFailure) {
        FriendEntry forCurrent = new FriendEntry(
                target.getUid(), target.getName(), target.getPhotoUrl());
        FriendEntry forTarget  = new FriendEntry(
                currentUser.getUid(), currentUser.getName(), currentUser.getPhotoUrl());

        WriteBatch batch = mDb.batch();
        batch.set(mDb.collection(USERS_COLLECTION).document(currentUser.getUid())
                        .collection(FRIENDS_SUB).document(target.getUid()), forCurrent);
        batch.set(mDb.collection(USERS_COLLECTION).document(target.getUid())
                        .collection(FRIENDS_SUB).document(currentUser.getUid()), forTarget);

        batch.commit()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not add friend. Try again."));
    }

    /**
     * Attaches a real-time listener to the friends subcollection of the given user.
     * Fires immediately with the current list, then again on any change.
     *
     * @return A {@link ListenerRegistration} — call {@code .remove()} in {@code onDestroyView()}.
     */
    public ListenerRegistration listenFriends(String uid,
                                              Consumer<List<FriendEntry>> onUpdate,
                                              Consumer<String> onError) {
        return mDb.collection(USERS_COLLECTION)
                .document(uid)
                .collection(FRIENDS_SUB)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load friends.");
                        return;
                    }
                    List<FriendEntry> entries = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            FriendEntry entry = doc.toObject(FriendEntry.class);
                            if (entry != null) entries.add(entry);
                        }
                    }
                    onUpdate.accept(entries);
                });
    }

    /**
     * One-shot fetch of the friends subcollection for the given user.
     * Use this for polling — call on a fixed interval instead of keeping a listener open.
     */
    public void fetchFriends(String uid,
                             Consumer<List<FriendEntry>> onSuccess,
                             Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION)
                .document(uid)
                .collection(FRIENDS_SUB)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FriendEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        FriendEntry entry = doc.toObject(FriendEntry.class);
                        if (entry != null) entries.add(entry);
                    }
                    onSuccess.accept(entries);
                })
                .addOnFailureListener(e -> onFailure.accept("Failed to load friends."));
    }

    /**
     * One-shot fetch of a single user document by UID.
     */
    public void fetchUserById(String uid,
                              Consumer<User> onSuccess,
                              Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        u.setUid(doc.getId());
                        onSuccess.accept(u);
                    } else {
                        onFailure.accept("User not found.");
                    }
                })
                .addOnFailureListener(e -> onFailure.accept("Could not load user."));
    }

    /**
     * Atomically removes a mutual friendship by deleting both subcollection entries.
     */
    public void removeFriend(String currentUid, String targetUid,
                             Runnable onSuccess, Consumer<String> onFailure) {
        WriteBatch batch = mDb.batch();
        batch.delete(mDb.collection(USERS_COLLECTION).document(currentUid)
                .collection(FRIENDS_SUB).document(targetUid));
        batch.delete(mDb.collection(USERS_COLLECTION).document(targetUid)
                .collection(FRIENDS_SUB).document(currentUid));
        batch.commit()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not remove friend."));
    }

    /**
     * One-shot fetch of all user documents (for the user search feature).
     * The caller is responsible for excluding the current user client-side.
     */
    public void fetchAllUsers(Consumer<List<User>> onSuccess, Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) users.add(user);
                    }
                    onSuccess.accept(users);
                })
                .addOnFailureListener(e -> onFailure.accept("Could not load users."));
    }

    /**
     * Fetches multiple users by uid.
     *
     * Firestore's {@code whereIn} supports up to 10 values, so this method batches when needed.
     */
    public void fetchUsersByIds(List<String> userIds,
                                Consumer<List<User>> onSuccess,
                                Consumer<String> onFailure) {
        if (userIds == null || userIds.isEmpty()) {
            onSuccess.accept(new ArrayList<>());
            return;
        }

        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i += 10) {
            int end = Math.min(i + 10, userIds.size());
            chunks.add(userIds.subList(i, end));
        }

        List<User> results = new ArrayList<>();
        final int[] pending = new int[]{chunks.size()};
        final boolean[] failed = new boolean[]{false};

        for (List<String> chunk : chunks) {
            mDb.collection(USERS_COLLECTION)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            User user = doc.toObject(User.class);
                            if (user != null) results.add(user);
                        }
                        pending[0]--;
                        if (pending[0] == 0 && !failed[0]) onSuccess.accept(results);
                    })
                    .addOnFailureListener(e -> {
                        if (failed[0]) return;
                        failed[0] = true;
                        onFailure.accept("Could not load attendees.");
                    });
        }
    }
}
