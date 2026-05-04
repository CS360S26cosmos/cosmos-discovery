package com.example.cosmos_discovery.database;

import android.util.Log;

import com.example.cosmos_discovery.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.example.cosmos_discovery.model.Review;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Provides event-related Firestore operations.
 *
 * All methods are async. Use {@code Consumer<T> onSuccess} for results and
 * {@code Consumer<String> onFailure} for error messages — the same pattern as
 * {@link AuthService}.
 */
public class EventService {

    private static final String TAG               = "EventService";
    private static final String EVENTS_COLLECTION = "events"; // Firestore collection name

    private final FirebaseFirestore mDb;

    /** Creates a new EventService backed by the default FirebaseFirestore instance. */
    public EventService() {
        mDb = FirebaseFirestore.getInstance();
    }

    // ── Fetch ─────────────────────────────────────────────────────────────

    /**
     * Attaches a real-time listener for approved events in the current calendar week
     * (Sunday 00:00 → Saturday 23:59:59). {@code onUpdate} fires immediately with the
     * current data, then again whenever a document is added, changed, or removed.
     *
     * NOTE: Requires a composite Firestore index on (status ASC, dateTime ASC).
     * On first run, Logcat will print a link — click it to auto-create the index.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}
     *         to detach and stop streaming updates.
     */
    public ListenerRegistration listenThisWeekEvents(Consumer<List<Event>> onUpdate,
                                                     Consumer<String> onError) {
        long[] weekBounds = currentWeekBounds();
        return mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .whereGreaterThanOrEqualTo("dateTime", weekBounds[0])
                .whereLessThanOrEqualTo("dateTime", weekBounds[1])
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load events.");
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Attaches a real-time listener for all approved events with a future dateTime,
     * ordered chronologically. Use this for a full "upcoming events" feed.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}
     */
    public ListenerRegistration listenUpcomingEvents(Consumer<List<Event>> onUpdate,
                                                     Consumer<String> onError) {
        long now = System.currentTimeMillis();
        return mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .whereGreaterThanOrEqualTo("dateTime", now)
                .orderBy("dateTime")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load events.");
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Attaches a real-time listener for all events where {@code userId} is in the
     * {@code attendeeIds} array (i.e. the user has RSVPed). Returns the full list
     * unsorted — callers should split and sort client-side into upcoming / past.
     *
     * Uses a single {@code array-contains} filter, which requires no composite index.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}
     */
    public ListenerRegistration listenMyRsvpedEvents(String userId,
                                                      Consumer<List<Event>> onUpdate,
                                                      Consumer<String> onError) {
        return mDb.collection(EVENTS_COLLECTION)
                .whereArrayContains("attendeeIds", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load your events.");
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /** One-time fetch of all events the user has RSVPed to — used by For You so that
     *  recommendations don't refresh mid-session when the user RSVPs. */
    public void fetchMyRsvpedEvents(String userId,
                                    Consumer<List<Event>> onSuccess,
                                    Consumer<String> onError) {
        mDb.collection(EVENTS_COLLECTION)
                .whereArrayContains("attendeeIds", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onSuccess.accept(events);
                })
                .addOnFailureListener(e -> onError.accept(
                        e.getMessage() != null ? e.getMessage() : "Failed to load your events."));
    }

    /**
     * Attaches a real-time listener for all events created by the given organizer.
     * No server-side ordering — callers sort client-side after splitting by status.
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onStop()}.
     */
    public ListenerRegistration listenOrganizerEvents(String organizerId,
                                                      Consumer<List<Event>> onUpdate,
                                                      Consumer<String> onError) {
        Log.d("EventService", "listenOrganizerEvents: organizerId=" + organizerId);
        return mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("EventService", "listenOrganizerEvents ERROR: " + error.getMessage(), error);
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to load your events.");
                        return;
                    }
                    Log.d("EventService", "listenOrganizerEvents: snapshot has "
                            + (snapshot != null ? snapshot.size() : "null") + " docs");
                    List<Event> events = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Event e = doc.toObject(Event.class);
                            if (e != null) {
                                e.setId(doc.getId());
                                events.add(e);
                            }
                        }
                    }
                    onUpdate.accept(events);
                });
    }

    /**
     * Fetches the {@code limit} most recently created approved events,
     * ordered by {@code createdAt} descending. Used for "Suggested" cards.
     */
    public void fetchRecentEvents(int limit,
                                  Consumer<List<Event>> onSuccess,
                                  Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onSuccess.accept(events);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Failed to load events."));
    }

    // ── Create / Delete / Update ──────────────────────────────────────────

    /**
     * Writes a new event document to Firestore. The document ID is auto-generated.
     * {@code onSuccess} receives the new document ID so callers can set {@code event.setId()}.
     */
    public void addEvent(Event event,
                         Consumer<String> onSuccess,
                         Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .add(event)
                .addOnSuccessListener(ref -> onSuccess.accept(ref.getId()))
                .addOnFailureListener(ex -> onFailure.accept("Could not create event. Try again."));
    }

    /**
     * Deletes the event document with the given ID.
     */
    public void removeEvent(String eventId,
                            Runnable onSuccess,
                            Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not delete event. Try again."));
    }

    /**
     * Updates the {@code status} field of an event. Use the constants on {@link Event}:
     * {@code Event.STATUS_APPROVED}, {@code Event.STATUS_PENDING}, {@code Event.STATUS_REJECTED}.
     */
    public void updateEventStatus(String eventId, String status,
                                  Runnable onSuccess,
                                  Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("status", status)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not update event status."));
    }

    /** Updates status and persists an optional rejection reason in a single Firestore write. */
    public void updateEventStatusWithReason(String eventId, String status, String reason,
                                            Runnable onSuccess, Consumer<String> onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        if (reason != null && !reason.trim().isEmpty()) {
            updates.put("rejectionReason", reason);
        }
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not update event status."));
    }

    /**
     * Cancels an event by setting its status to {@link Event#STATUS_CANCELLED} and
     * fanning out a {@link com.example.cosmos_discovery.model.Notification#TYPE_EVENT_CANCELLED}
     * notification to every attendee (and to the organizer when the canceller is someone else,
     * e.g. an admin).
     *
     * @param eventId          The event document ID.
     * @param cancelledByUid   UID of whoever performed the cancellation; used to skip self-notify.
     * @param onSuccess        Called after the status update succeeds (notif fan-out is fire-and-forget).
     * @param onFailure        Called with an error message if the status update fails.
     */
    public void cancelEvent(String eventId, String cancelledByUid,
                            Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    com.example.cosmos_discovery.model.Event ev = doc.toObject(
                            com.example.cosmos_discovery.model.Event.class);
                    if (ev == null) {
                        onFailure.accept("Event not found.");
                        return;
                    }
                    ev.setId(doc.getId());
                    mDb.collection(EVENTS_COLLECTION).document(eventId)
                            .update("status", Event.STATUS_CANCELLED)
                            .addOnSuccessListener(unused -> {
                                fireCancellationNotifs(ev, cancelledByUid);
                                onSuccess.run();
                            })
                            .addOnFailureListener(ex -> onFailure.accept("Could not cancel event."));
                })
                .addOnFailureListener(ex -> onFailure.accept("Could not load event to cancel."));
    }

    private void fireCancellationNotifs(com.example.cosmos_discovery.model.Event ev,
                                        String cancelledByUid) {
        com.example.cosmos_discovery.database.NotificationService notifs =
                new com.example.cosmos_discovery.database.NotificationService();

        // Attendees get a personal-perspective cancellation notif.
        List<String> attendees = new ArrayList<>();
        if (ev.getAttendeeIds() != null) attendees.addAll(ev.getAttendeeIds());
        if (cancelledByUid != null) attendees.remove(cancelledByUid);
        if (ev.getOrganizerId() != null) attendees.remove(ev.getOrganizerId());

        if (!attendees.isEmpty()) {
            com.example.cosmos_discovery.model.Notification np =
                    new com.example.cosmos_discovery.model.Notification(
                        com.example.cosmos_discovery.model.Notification.TYPE_EVENT_CANCELLED,
                        "Event Cancelled",
                        "\"" + ev.getTitle() + "\" has been cancelled.",
                        System.currentTimeMillis());
            np.setEventId(ev.getId());
            np.setEventTitle(ev.getTitle());
            np.setAudience(com.example.cosmos_discovery.model.Notification.AUDIENCE_PERSONAL);
            notifs.writeNotificationToUsers(attendees,
                    "event_cancelled_" + ev.getId(), np, () -> {}, err -> {});
        }

        // Organizer gets an organizer-perspective notif (only when admin/someone-else cancelled).
        if (ev.getOrganizerId() != null
                && !ev.getOrganizerId().equals(cancelledByUid)) {
            com.example.cosmos_discovery.model.Notification no =
                    new com.example.cosmos_discovery.model.Notification(
                        com.example.cosmos_discovery.model.Notification.TYPE_EVENT_CANCELLED,
                        "Your event was cancelled",
                        "\"" + ev.getTitle() + "\" was cancelled by an admin.",
                        System.currentTimeMillis());
            no.setEventId(ev.getId());
            no.setEventTitle(ev.getTitle());
            no.setAudience(com.example.cosmos_discovery.model.Notification.AUDIENCE_ORGANIZER);
            notifs.writeNotificationToUsers(
                    java.util.Collections.singletonList(ev.getOrganizerId()),
                    "event_cancelled_org_" + ev.getId(), no, () -> {}, err -> {});
        }
    }

    /**
     * Updates arbitrary fields on an event document.
     */
    public void updateEvent(String eventId,
                            Map<String, Object> fields,
                            Runnable onSuccess,
                            Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(fields)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not update event."));
    }

    /**
     * One-shot fetch of all approved upcoming events ordered chronologically.
     * Use this for polling — call on a fixed interval instead of keeping a listener open.
     */
    public void fetchUpcomingEvents(Consumer<List<Event>> onSuccess,
                                    Consumer<String> onFailure) {
        long now = System.currentTimeMillis();
        mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("status", Event.STATUS_APPROVED)
                .whereGreaterThanOrEqualTo("dateTime", now)
                .orderBy("dateTime")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            events.add(e);
                        }
                    }
                    onSuccess.accept(events);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Failed to load events."));
    }

    /**
     * Attaches a real-time listener on a single event document.
     * Fires immediately and on every subsequent change (RSVP, check-in, etc.).
     *
     * @return a {@link ListenerRegistration} — call {@code .remove()} in {@code onDestroy()}.
     */
    public ListenerRegistration listenEvent(String eventId,
                                            Consumer<Event> onUpdate,
                                            Consumer<String> onError) {
        return mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.accept(error.getMessage() != null
                                ? error.getMessage() : "Failed to listen for event updates.");
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Event e = snapshot.toObject(Event.class);
                        if (e != null) {
                            e.setId(snapshot.getId());
                            onUpdate.accept(e);
                        }
                    }
                });
    }

    /**
     * Fetches a single event by its document ID. One-shot read.
     */
    public void fetchEventById(String eventId,
                               Consumer<Event> onSuccess,
                               Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Event e = doc.toObject(Event.class);
                    if (e != null) {
                        e.setId(doc.getId());
                        onSuccess.accept(e);
                    } else {
                        onFailure.accept("Event not found.");
                    }
                })
                .addOnFailureListener(ex -> onFailure.accept("Could not fetch event."));
    }

    // ── RSVP ──────────────────────────────────────────────────────────────

    /**
     * Adds {@code userId} to the event's {@code attendeeIds} array and increments
     * {@code rsvpCount} by 1, enforcing the event's capacity limit.
     *
     * Uses a Firestore transaction to atomically read-then-write, preventing
     * overbooking when multiple users RSVP concurrently.
     */
    public void rsvpToEvent(String eventId, String userId,
                            Runnable onSuccess, Consumer<String> onFailure) {
        mDb.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(
                    mDb.collection(EVENTS_COLLECTION).document(eventId));

            List<String> attendeeIds = (List<String>) snapshot.get("attendeeIds");
            List<String> newIds = attendeeIds != null
                    ? new ArrayList<>(attendeeIds) : new ArrayList<>();

            if (newIds.contains(userId)) {
                return null; // already RSVPed — idempotent success
            }

            Long capLong = snapshot.getLong("capacity");
            long capacity = capLong != null ? capLong : 0;

            if (capacity > 0 && newIds.size() >= capacity) {
                throw new FirebaseFirestoreException(
                        "This event is full.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            newIds.add(userId);
            transaction.update(
                    mDb.collection(EVENTS_COLLECTION).document(eventId),
                    "attendeeIds", newIds,
                    "rsvpCount",   newIds.size());

            Map<String, Object> rsvpDoc = new HashMap<>();
            rsvpDoc.put("uid", userId);
            rsvpDoc.put("timestamp", System.currentTimeMillis());
            transaction.set(
                    mDb.collection(EVENTS_COLLECTION).document(eventId)
                            .collection("rsvps").document(userId),
                    rsvpDoc);
            return null;
        })
        .addOnSuccessListener(unused -> onSuccess.run())
        .addOnFailureListener(ex -> onFailure.accept(
                ex.getMessage() != null ? ex.getMessage() : "Could not RSVP. Try again."));
    }

    /**
     * Removes {@code userId} from the event's {@code attendeeIds} array and decrements
     * {@code rsvpCount} by 1. Both updates are applied atomically.
     */
    public void cancelRsvp(String eventId, String userId,
                           Runnable onSuccess, Consumer<String> onFailure) {
        mDb.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(
                    mDb.collection(EVENTS_COLLECTION).document(eventId));
            List<String> attendeeIds = (List<String>) snapshot.get("attendeeIds");
            List<String> newIds = attendeeIds != null
                    ? new ArrayList<>(attendeeIds) : new ArrayList<>();
            newIds.remove(userId);
            transaction.update(
                    mDb.collection(EVENTS_COLLECTION).document(eventId),
                    "attendeeIds", newIds,
                    "rsvpCount",   newIds.size());
            transaction.delete(
                    mDb.collection(EVENTS_COLLECTION).document(eventId)
                            .collection("rsvps").document(userId));
            return null;
        })
        .addOnSuccessListener(unused -> onSuccess.run())
        .addOnFailureListener(ex -> onFailure.accept("Could not cancel RSVP. Try again."));
    }

    /**
     * Fetches RSVP timestamps for an event from the {@code rsvps} subcollection,
     * sorted ascending. Used by EventStats for the per-day timeline chart.
     */
    public void getRsvpTimestamps(String eventId,
                                  Consumer<List<Long>> onSuccess,
                                  Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION).document(eventId)
                .collection("rsvps")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Long> out = new ArrayList<>(snap.size());
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Long ts = d.getLong("timestamp");
                        if (ts != null) out.add(ts);
                    }
                    onSuccess.accept(out);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Could not load RSVP timestamps."));
    }

    // ── Check-in ──────────────────────────────────────────────────────────

    /**
     * Adds {@code userId} to the event's {@code checkedInIds} array.
     * Uses {@code FieldValue.arrayUnion} for atomic, idempotent writes.
     */
    public void checkInAttendee(String eventId, String userId,
                                Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("checkedInIds", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not check in attendee."));
    }

    /**
     * Removes {@code userId} from the event's {@code checkedInIds} array.
     * Uses {@code FieldValue.arrayRemove} for atomic writes.
     */
    public void removeCheckIn(String eventId, String userId,
                              Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("checkedInIds", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not remove check-in."));
    }

    // ── Categories ────────────────────────────────────────────────────────

    /**
     * One-shot fetch of all category names from the {@code categories} collection.
     * Each document must have a {@code name} String field. Results are sorted alphabetically.
     * Used to populate the filter overlay's category chips.
     */
    public void fetchCategories(Consumer<List<String>> onSuccess, Consumer<String> onFailure) {
        mDb.collection("categories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) names.add(name);
                    }
                    Collections.sort(names);
                    onSuccess.accept(names);
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Failed to load categories."));
    }

    // ── Category management ───────────────────────────────────────────────

    public ListenerRegistration listenCategories(
            Consumer<List<DocumentSnapshot>> onUpdate, Consumer<String> onFailure) {
        return mDb.collection("categories")
                .orderBy("name")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        onFailure.accept("Could not load categories.");
                        return;
                    }
                    onUpdate.accept(snap != null ? snap.getDocuments() : new ArrayList<>());
                });
    }

    public void addCategory(String name, Runnable onSuccess, Consumer<String> onFailure) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        mDb.collection("categories").add(data)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not add category."));
    }

    public void deleteCategory(String docId, Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection("categories").document(docId).delete()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> onFailure.accept("Could not delete category."));
    }

    // ── Ratings ───────────────────────────────────────────────────────────

    /**
     * Saves a rating (1–5) for a user on an event.
     * Stored as events/{eventId}.ratings.{userId} so it uses the same
     * collection permissions as RSVP/check-in writes — no separate rules needed.
     */
    public void saveRating(String eventId, String userId, int rating,
                           Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("ratings." + userId, rating)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "saveRating failed: " + ex.getMessage(), ex);
                    onFailure.accept("Could not save rating.");
                });
    }

    /**
     * Fetches the user's saved rating for an event.
     * Calls {@code onSuccess} with the rating (1–5), or {@code null} if not yet rated.
     */
    public void fetchRating(String eventId, String userId,
                            Consumer<Integer> onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long value = doc.getLong("ratings." + userId);
                    onSuccess.accept(value != null ? value.intValue() : null);
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "fetchRating failed: " + ex.getMessage(), ex);
                    onFailure.accept("Could not fetch rating.");
                });
    }

    /**
     * Computes the average rating for an event across all raters.
     * Calls {@code onSuccess} with the average (1.0–5.0), or {@code null} if no ratings exist.
     */
    public void fetchAverageRating(String eventId,
                                   Consumer<Double> onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ratingsMap = (Map<String, Object>) doc.get("ratings");
                    if (ratingsMap == null || ratingsMap.isEmpty()) {
                        onSuccess.accept(null);
                        return;
                    }
                    double sum = 0;
                    int count = 0;
                    for (Object val : ratingsMap.values()) {
                        if (val instanceof Long) {
                            sum += (Long) val;
                            count++;
                        } else if (val instanceof Double) {
                            sum += (Double) val;
                            count++;
                        }
                    }
                    onSuccess.accept(count > 0 ? sum / count : null);
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "fetchAverageRating failed: " + ex.getMessage(), ex);
                    onFailure.accept("Could not fetch average rating.");
                });
    }

    /**
     * Fetches the aggregate average rating for an organizer across all their events.
     * Calls onSuccess with the average (1.0–5.0), or null if no ratings exist.
     */
    public void fetchOrganizerAverageRating(String organizerId,
                                            Consumer<Double> onSuccess,
                                            Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(snap -> {
                    double sum = 0;
                    int count = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ratingsMap =
                                (Map<String, Object>) doc.get("ratings");
                        if (ratingsMap == null) continue;
                        for (Object val : ratingsMap.values()) {
                            if (val instanceof Long) {
                                sum += (Long) val;
                                count++;
                            } else if (val instanceof Double) {
                                sum += (Double) val;
                                count++;
                            }
                        }
                    }
                    onSuccess.accept(count > 0 ? sum / count : null);
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "fetchOrganizerAverageRating failed: " + ex.getMessage(), ex);
                    onFailure.accept("Could not fetch organizer rating.");
                });
    }

    /**
     * Saves a text review for a user on an event.
     * Stored as events/{eventId}.reviews.{userId} alongside the ratings map.
     */
    public void saveReview(String eventId, String userId, String review,
                           Runnable onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("reviews." + userId, review)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "saveReview failed: " + ex.getMessage(), ex);
                    onFailure.accept("Could not save review.");
                });
    }

    /**
     * Fetches the user's saved review text for an event.
     * Calls {@code onSuccess} with the review string, or {@code null} if none exists yet.
     */
    public void fetchReview(String eventId, String userId,
                            Consumer<String> onSuccess, Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Object val = doc.get("reviews." + userId);
                    onSuccess.accept(val instanceof String ? (String) val : null);
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "fetchReview failed: " + ex.getMessage(), ex);
                    onFailure.accept("Could not fetch review.");
                });
    }

    /**
     * Fetches all reviews for an event, joining each reviewer's display name from
     * the users collection. Calls {@code onSuccess} with a list of {@link Review}
     * objects — one per user who has a stored review entry (text may be empty).
     */
    public void fetchAllReviews(String eventId,
                                Consumer<List<Review>> onSuccess,
                                Consumer<String> onFailure) {
        mDb.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reviewsMap = (Map<String, Object>) doc.get("reviews");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ratingsMap = (Map<String, Object>) doc.get("ratings");

                    if (reviewsMap == null || reviewsMap.isEmpty()) {
                        onSuccess.accept(new ArrayList<>());
                        return;
                    }

                    List<String> userIds = new ArrayList<>(reviewsMap.keySet());
                    List<Review> results = Collections.synchronizedList(new ArrayList<>());
                    AtomicInteger remaining = new AtomicInteger(userIds.size());

                    for (String uid : userIds) {
                        String text = reviewsMap.get(uid) instanceof String
                                ? (String) reviewsMap.get(uid) : "";
                        final String finalText = text;

                        int rating = 0;
                        if (ratingsMap != null && ratingsMap.get(uid) instanceof Long) {
                            rating = ((Long) ratingsMap.get(uid)).intValue();
                        }
                        final int finalRating = rating;

                        mDb.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    results.add(new Review(uid, name != null ? name : "Unknown",
                                            finalRating, finalText));
                                    if (remaining.decrementAndGet() == 0) onSuccess.accept(results);
                                })
                                .addOnFailureListener(ex -> {
                                    results.add(new Review(uid, "Unknown", finalRating, finalText));
                                    if (remaining.decrementAndGet() == 0) onSuccess.accept(results);
                                });
                    }
                })
                .addOnFailureListener(ex -> onFailure.accept(
                        ex.getMessage() != null ? ex.getMessage() : "Could not load reviews."));
    }

    /** Returns [weekStartMs, weekEndMs] for the current Sun–Sat week. */
    private long[] currentWeekBounds() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_WEEK, 6);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }
}
