package com.example.cosmos_discovery.util;

import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;

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

    private EventService mService; // lazily initialized — keeps RsvpHandler testable without Firebase

    public RsvpHandler() {}

    private EventService service() {
        if (mService == null) mService = new EventService();
        return mService;
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
            event.setRsvpCount(event.getRsvpCount() + 1);
            onRefresh.run();
        };

        // Skip Firestore write for local-only / dummy events (no real document ID yet)
        if (event.getId() == null || event.getId().isEmpty()) {
            applyLocally.run();
            return;
        }
        service().rsvpToEvent(event.getId(), uid, applyLocally, onError);
    }

    private void cancelRsvp(Event event, String uid, Runnable onRefresh, Consumer<String> onError) {
        Runnable applyLocally = () -> {
            ArrayList<String> ids = event.getAttendeeIds() != null
                    ? new ArrayList<>(event.getAttendeeIds())
                    : new ArrayList<>();
            ids.remove(uid);
            event.setAttendeeIds(ids);
            event.setRsvpCount(Math.max(0, event.getRsvpCount() - 1));
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
