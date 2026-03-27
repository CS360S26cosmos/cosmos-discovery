package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.EventSmallAdapter;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.util.RoleUtil;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the current student's RSVPed events split into two sections:
 * upcoming events (dateTime in the future) and past events (dateTime in the past).
 *
 * Uses a single Firestore {@code array-contains} listener on {@code attendeeIds} and
 * splits the result client-side. The listener fires in real-time, so cancelling an
 * RSVP from this screen removes the event immediately.
 */
public class MyEventsFragment extends Fragment
        implements EventSmallAdapter.OnRsvpClickListener {

    private final EventService mEventService = new EventService();
    private final RsvpHandler  mRsvpHandler  = new RsvpHandler();

    private ListenerRegistration mListener;        // detached in onStop()

    private EventSmallAdapter mUpcomingAdapter;    // showRsvp = true
    private EventSmallAdapter mPastAdapter;        // showRsvp = false

    private final List<Event> mUpcomingEvents = new ArrayList<>(); // dateTime >= now, approved
    private final List<Event> mPastEvents     = new ArrayList<>(); // dateTime <  now, approved

    private TextView mTvEmptyUpcoming; // shown when mUpcomingEvents is empty
    private TextView mTvEmptyPast;     // shown when mPastEvents is empty

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_events, container, false);

        mTvEmptyUpcoming = root.findViewById(R.id.tvEmptyUpcoming);
        mTvEmptyPast     = root.findViewById(R.id.tvEmptyPast);

        setupRecyclerViews(root);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mListener != null) {
            mListener.remove();
            mListener = null;
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    /** Initialises both RecyclerViews and attaches their adapters. */
    private void setupRecyclerViews(View root) {
        RecyclerView rvUpcoming = root.findViewById(R.id.recyclerViewUpcoming);
        RecyclerView rvPast     = root.findViewById(R.id.recyclerViewPast);

        mUpcomingAdapter = new EventSmallAdapter(requireContext(), mUpcomingEvents, this);
        mPastAdapter     = new EventSmallAdapter(requireContext(), mPastEvents, null, false);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUpcoming.setAdapter(mUpcomingAdapter);

        rvPast.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPast.setAdapter(mPastAdapter);
    }

    // ── Firestore listener ────────────────────────────────────────────────

    /**
     * Attaches the Firestore real-time listener for the current user's RSVPed events.
     * No-op if no user is signed in.
     */
    private void attachListener() {
        String uid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid()
                : null;
        if (uid == null) return;

        mListener = mEventService.listenMyRsvpedEvents(uid, this::onEventsUpdate, err -> {});
    }

    /**
     * Splits the full RSVPed-events list into upcoming and past by comparing each
     * event's {@code dateTime} to the current time. Only approved events are shown.
     * Upcoming events are sorted by nearest date first; past events by most recent first.
     */
    private void onEventsUpdate(List<Event> allRsvped) {
        long now = System.currentTimeMillis();

        mUpcomingEvents.clear();
        mPastEvents.clear();

        for (Event e : allRsvped) {
            if (!Event.STATUS_APPROVED.equals(e.getStatus())) continue;
            if (e.getDateTime() >= now) {
                mUpcomingEvents.add(e);
            } else {
                mPastEvents.add(e);
            }
        }

        // Upcoming: soonest first
        mUpcomingEvents.sort((a, b) -> Long.compare(a.getDateTime(), b.getDateTime()));
        // Past: most recent first
        mPastEvents.sort((a, b) -> Long.compare(b.getDateTime(), a.getDateTime()));

        mUpcomingAdapter.updateData(mUpcomingEvents);
        mPastAdapter.updateData(mPastEvents);

        mTvEmptyUpcoming.setVisibility(mUpcomingEvents.isEmpty() ? View.VISIBLE : View.GONE);
        mTvEmptyPast.setVisibility(mPastEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── RSVP click (upcoming events only) ────────────────────────────────

    /**
     * Delegates RSVP toggle to {@link RsvpHandler}. Only wired for upcoming events;
     * the past adapter has {@code showRsvp = false} so this is never called for past cards.
     * Errors are intentionally silent — the real-time Firestore listener will self-correct.
     */
    @Override
    public void onRsvpClick(Event event, int position) {
        mRsvpHandler.toggle(
                event,
                () -> mUpcomingAdapter.notifyItemChanged(position),
                err -> {} // silent — real-time listener will self-correct
        );
    }
}
