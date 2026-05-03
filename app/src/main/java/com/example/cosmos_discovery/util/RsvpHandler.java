package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.NotificationService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.Notification;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Reusable RSVP toggle logic for any screen that shows event cards.
 *
 * Call {@link #toggle} from any Activity or Fragment's RSVP click listener.
 * On Firestore success the local Event object is updated optimistically and
 * {@code onRefresh} is called so the caller can re-bind just that row
 * (e.g. adapter.notifyItemChanged(position)).
 */
public class RsvpHandler {

    private EventService        mService;       // lazily initialized — keeps RsvpHandler testable without Firebase
    private NotificationService mNotifService;  // lazily initialized — same reason

    public RsvpHandler() {}

    private EventService service() {
        if (mService == null) mService = new EventService();
        return mService;
    }

    private NotificationService notifService() {
        if (mNotifService == null) mNotifService = new NotificationService();
        return mNotifService;
    }

    /**
     * Toggles the RSVP state of {@code event} for the current user.
     *
     * @param event     The event to RSVP to or cancel.
     * @param onRefresh Called on the main thread after a successful Firestore write
     *                  — use this to call adapter.notifyItemChanged(position).
     * @param onError   Called with a human-readable error message on failure.
     */
    public void toggle(Event event, Runnable onRefresh, Consumer<String> onError) {
        String uid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid()
                : null;

        if (uid == null) {
            onError.accept("You must be logged in to RSVP.");
            return;
        }

        if (event.isRsvped(uid)) {
            cancelRsvp(event, uid, onRefresh, onError);
        } else {
            addRsvp(event, uid, onRefresh, onError);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void addRsvp(Event event, String uid, Runnable onRefresh, Consumer<String> onError) {
        if (event.isRegistrationClosed()) {
            onError.accept("Registration is closed for this event.");
            return;
        }
        if (event.isFull()) {
            onError.accept("This event is full.");
            return;
        }

        Runnable applyLocally = () -> {
            ArrayList<String> ids = event.getAttendeeIds() != null
                    ? new ArrayList<>(event.getAttendeeIds())
                    : new ArrayList<>();
            ids.add(uid);
            event.setAttendeeIds(ids);
            event.setRsvpCount(ids.size());
            onRefresh.run();
        };

        // Skip Firestore write for local-only / dummy events (no real document ID yet)
        if (event.getId() == null || event.getId().isEmpty()) {
            applyLocally.run();
            return;
        }
        service().rsvpToEvent(event.getId(), uid, () -> {
            applyLocally.run();
            Notification notif = new Notification(
                    Notification.TYPE_RSVP_CONFIRMED,
                    "RSVP Confirmed",
                    "You have successfully RSVPed to " + event.getTitle() + "!",
                    System.currentTimeMillis()
            );
            notif.setEventId(event.getId());
            notif.setEventTitle(event.getTitle());
            notif.setAudience(Notification.AUDIENCE_PERSONAL);
            notifService().writeNotification(uid, notif, () -> {}, err -> {});

            // Ping the organizer that someone RSVPed (skip self-RSVP).
            if (event.getOrganizerId() != null
                    && !event.getOrganizerId().isEmpty()
                    && !event.getOrganizerId().equals(uid)) {
                String attendeeName = RoleUtil.getCurrentUser() != null
                        ? RoleUtil.getCurrentUser().getName() : null;
                String displayName = attendeeName != null && !attendeeName.isEmpty()
                        ? attendeeName : "Someone";
                Notification rsvpNotif = new Notification(
                        Notification.TYPE_RSVP_RECEIVED,
                        "New RSVP",
                        displayName + " RSVPed to \"" + event.getTitle() + "\".",
                        System.currentTimeMillis());
                rsvpNotif.setEventId(event.getId());
                rsvpNotif.setEventTitle(event.getTitle());
                rsvpNotif.setAudience(Notification.AUDIENCE_ORGANIZER);
                notifService().writeNotification(event.getOrganizerId(), rsvpNotif,
                        () -> {}, err -> {});
            }

            // If this RSVP just filled the event, ping the organizer (once).
            if (event.getCapacity() > 0
                    && event.getRsvpCount() >= event.getCapacity()
                    && event.getOrganizerId() != null
                    && !event.getOrganizerId().isEmpty()
                    && !event.getOrganizerId().equals(uid)) {
                Notification organizerNotif = new Notification(
                        Notification.TYPE_CAPACITY_FULL,
                        "Your event is full",
                        "\"" + event.getTitle() + "\" has reached its capacity of "
                                + event.getCapacity() + ".",
                        System.currentTimeMillis());
                organizerNotif.setEventId(event.getId());
                organizerNotif.setEventTitle(event.getTitle());
                organizerNotif.setAudience(Notification.AUDIENCE_ORGANIZER);
                // Fixed doc ID per event → idempotent; only one capacity-full notif per event.
                String docId = "capacity_full_" + event.getId();
                notifService().writeNotificationToUsers(
                        java.util.Collections.singletonList(event.getOrganizerId()),
                        docId, organizerNotif, () -> {}, err -> {});
            }
        }, onError);
    }

    private void cancelRsvp(Event event, String uid, Runnable onRefresh, Consumer<String> onError) {
        Runnable applyLocally = () -> {
            ArrayList<String> ids = event.getAttendeeIds() != null
                    ? new ArrayList<>(event.getAttendeeIds())
                    : new ArrayList<>();
            ids.remove(uid);
            event.setAttendeeIds(ids);
            event.setRsvpCount(ids.size());
            onRefresh.run();
        };

        // Skip Firestore write for local-only / dummy events (no real document ID yet)
        if (event.getId() == null || event.getId().isEmpty()) {
            applyLocally.run();
            return;
        }
        service().cancelRsvp(event.getId(), uid, applyLocally, onError);
    }
}
