package com.example.cosmos_discovery.database;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * One-time migration that backfills the {@code events/{id}/rsvps} subcollection
 * for events whose RSVPs predate per-RSVP timestamp tracking.
 *
 * For each event, distributes {@code attendeeIds} evenly across
 * {@code [createdAt, min(now, dateTime)]}. Idempotent: skips events whose
 * subcollection is already non-empty.
 */
public class RsvpBackfillMigration {

    private static final String TAG = "RsvpBackfill";
    private static final String META_DOC = "rsvp_backfill_v1";
    private static final int BATCH_LIMIT = 500;

    private final FirebaseFirestore db;

    public RsvpBackfillMigration() {
        db = FirebaseFirestore.getInstance();
    }

    public void run(Consumer<String> onSuccess, Consumer<String> onFailure) {
        db.collection("meta").document(META_DOC).get()
                .addOnSuccessListener(meta -> {
                    if (meta.exists() && meta.getLong("completedAt") != null) {
                        onSuccess.accept("Backfill already complete.");
                        return;
                    }
                    fetchEvents(onSuccess, onFailure);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Could not read meta."));
    }

    private void fetchEvents(Consumer<String> onSuccess, Consumer<String> onFailure) {
        db.collection("events").get()
                .addOnSuccessListener(snap -> processEvents(snap, onSuccess, onFailure))
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Could not load events."));
    }

    private void processEvents(QuerySnapshot snap,
                               Consumer<String> onSuccess,
                               Consumer<String> onFailure) {
        List<DocumentSnapshot> docs = snap.getDocuments();
        if (docs.isEmpty()) {
            markComplete(onSuccess, onFailure, 0, 0);
            return;
        }
        AtomicInteger remaining   = new AtomicInteger(docs.size());
        AtomicInteger eventsDone  = new AtomicInteger(0);
        AtomicInteger rsvpsWritten = new AtomicInteger(0);

        for (DocumentSnapshot eventDoc : docs) {
            backfillOneEvent(eventDoc,
                    written -> {
                        eventsDone.incrementAndGet();
                        rsvpsWritten.addAndGet(written);
                        if (remaining.decrementAndGet() == 0) {
                            markComplete(onSuccess, onFailure,
                                    eventsDone.get(), rsvpsWritten.get());
                        }
                    },
                    err -> {
                        Log.w(TAG, "Failed event " + eventDoc.getId() + ": " + err);
                        if (remaining.decrementAndGet() == 0) {
                            markComplete(onSuccess, onFailure,
                                    eventsDone.get(), rsvpsWritten.get());
                        }
                    });
        }
    }

    @SuppressWarnings("unchecked")
    private void backfillOneEvent(DocumentSnapshot eventDoc,
                                  Consumer<Integer> onDone,
                                  Consumer<String> onFailure) {
        String eventId = eventDoc.getId();
        List<String> attendeeIds = (List<String>) eventDoc.get("attendeeIds");
        if (attendeeIds == null || attendeeIds.isEmpty()) {
            onDone.accept(0);
            return;
        }
        Long createdAt = eventDoc.getLong("createdAt");
        Long dateTime  = eventDoc.getLong("dateTime");
        long now = System.currentTimeMillis();
        long startTmp = createdAt != null ? createdAt : now;
        long endTmp   = Math.min(now, dateTime != null ? dateTime : now);
        if (endTmp <= startTmp) endTmp = startTmp + 1;
        final long start = startTmp;
        final long end = endTmp;

        // Skip if subcollection already has any docs (idempotency).
        db.collection("events").document(eventId).collection("rsvps")
                .limit(1).get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        onDone.accept(0);
                        return;
                    }
                    writeBackfill(eventId, attendeeIds, start, end, onDone, onFailure);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "subcollection check failed"));
    }

    private void writeBackfill(String eventId, List<String> attendeeIds,
                               long start, long end,
                               Consumer<Integer> onDone, Consumer<String> onFailure) {
        int n = attendeeIds.size();
        long span = end - start;

        AtomicInteger batchesRemaining = new AtomicInteger(0);
        AtomicInteger written = new AtomicInteger(0);
        int total = 0;

        WriteBatch batch = db.batch();
        int inBatch = 0;
        for (int i = 0; i < n; i++) {
            String uid = attendeeIds.get(i);
            long ts = start + (long) (span * (double) (i + 1) / (double) (n + 1));
            Map<String, Object> doc = new HashMap<>();
            doc.put("uid", uid);
            doc.put("timestamp", ts);
            batch.set(
                    db.collection("events").document(eventId)
                            .collection("rsvps").document(uid),
                    doc);
            inBatch++;
            total++;
            if (inBatch == BATCH_LIMIT) {
                batchesRemaining.incrementAndGet();
                final int chunkSize = inBatch;
                batch.commit()
                        .addOnSuccessListener(u -> {
                            written.addAndGet(chunkSize);
                            if (batchesRemaining.decrementAndGet() == 0) {
                                onDone.accept(written.get());
                            }
                        })
                        .addOnFailureListener(ex -> onFailure.accept(
                                ex.getMessage() != null ? ex.getMessage() : "batch commit failed"));
                batch = db.batch();
                inBatch = 0;
            }
        }
        if (inBatch > 0) {
            batchesRemaining.incrementAndGet();
            final int chunkSize = inBatch;
            batch.commit()
                    .addOnSuccessListener(u -> {
                        written.addAndGet(chunkSize);
                        if (batchesRemaining.decrementAndGet() == 0) {
                            onDone.accept(written.get());
                        }
                    })
                    .addOnFailureListener(ex -> onFailure.accept(
                            ex.getMessage() != null ? ex.getMessage() : "batch commit failed"));
        } else if (total == 0) {
            onDone.accept(0);
        }
    }

    private void markComplete(Consumer<String> onSuccess, Consumer<String> onFailure,
                              int eventsDone, int rsvpsWritten) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("completedAt", System.currentTimeMillis());
        meta.put("eventsProcessed", eventsDone);
        meta.put("rsvpsWritten", rsvpsWritten);
        db.collection("meta").document(META_DOC).set(meta)
                .addOnSuccessListener(u -> onSuccess.accept(
                        "Backfill complete: " + eventsDone + " events, " + rsvpsWritten + " RSVPs."))
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Could not write meta."));
    }
}
