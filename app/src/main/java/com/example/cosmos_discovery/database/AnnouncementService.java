package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.Announcement;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads and writes per-event announcements and fans out a push-bound
 * notification to every RSVP'd attendee.
 *
 * Stored at {@code events/{eventId}/announcements/{auto-id}}.
 */
public class AnnouncementService {

    private static final String EVENTS_COL        = "events";
    private static final String ANNOUNCEMENTS_SUB = "announcements";

    private final FirebaseFirestore   mDb           = FirebaseFirestore.getInstance();
    private final EventService        mEventService = new EventService();
    private final NotificationService mNotifService = new NotificationService();

    /**
     * Persists the announcement under the event's subcollection, then writes one
     * {@link Notification#TYPE_ANNOUNCEMENT} doc per attendee so the FCM Cloud
     * Function delivers the push. Organizer is excluded from the recipient list.
     */
    public void sendAnnouncement(String eventId, String message, String organizerId,
                                 Runnable onSuccess, Consumer<String> onError) {
        if (message == null || message.trim().isEmpty()) {
            onError.accept("Announcement message is required.");
            return;
        }
        if (message.length() > Announcement.MAX_LENGTH) {
            onError.accept("Announcement is too long (max " + Announcement.MAX_LENGTH + " characters).");
            return;
        }

        Announcement a = new Announcement(message.trim(), System.currentTimeMillis(), organizerId);

        mDb.collection(EVENTS_COL).document(eventId)
                .collection(ANNOUNCEMENTS_SUB)
                .add(a)
                .addOnSuccessListener(ref -> {
                    fanOut(eventId, message.trim(), organizerId);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onError.accept("Could not send announcement."));
    }

    private void fanOut(String eventId, String message, String organizerId) {
        mEventService.fetchEventById(eventId, ev -> {
            if (ev.getAttendeeIds() == null || ev.getAttendeeIds().isEmpty()) return;

            List<String> recipients = new ArrayList<>(ev.getAttendeeIds());
            if (organizerId != null) recipients.remove(organizerId);
            if (recipients.isEmpty()) return;

            Notification n = new Notification(
                    Notification.TYPE_ANNOUNCEMENT,
                    "Announcement: " + ev.getTitle(),
                    message,
                    System.currentTimeMillis());
            n.setEventId(eventId);
            n.setEventTitle(ev.getTitle());
            n.setAudience(Notification.AUDIENCE_PERSONAL);
            mNotifService.writeNotificationToUsers(recipients, null, n, () -> {}, err -> {});
        }, err -> { /* fan-out is best-effort; the announcement doc was already saved */ });
    }

    /** Real-time listener for announcements on one event, newest-first. */
    public ListenerRegistration listenAnnouncements(String eventId,
                                                    Consumer<List<Announcement>> onUpdate,
                                                    Consumer<String> onError) {
        return mDb.collection(EVENTS_COL).document(eventId)
                .collection(ANNOUNCEMENTS_SUB)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        onError.accept(err.getMessage() != null
                                ? err.getMessage() : "Could not load announcements.");
                        return;
                    }
                    List<Announcement> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Announcement a = doc.toObject(Announcement.class);
                            if (a != null) {
                                a.setId(doc.getId());
                                list.add(a);
                            }
                        }
                    }
                    onUpdate.accept(list);
                });
    }
}
