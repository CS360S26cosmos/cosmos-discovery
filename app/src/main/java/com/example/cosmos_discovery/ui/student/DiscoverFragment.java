package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.EventBigAdapter;
import com.example.cosmos_discovery.adapter.EventSmallAdapter;
import com.example.cosmos_discovery.ui.organizer.EventDetailsActivity;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.util.DismissedEventsStore;
import com.example.cosmos_discovery.util.RecommendationEngine;
import com.example.cosmos_discovery.util.RoleUtil;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Discover screen for the student role.
 *
 * Two sections:
 *   • "Suggested"  — horizontal carousel of big cards ({@code recyclerViewSuggested})
 *   • "This Week"  — vertical list of small cards  ({@code recyclerViewThisWeek})
 *
 * Each section shows an empty-state message when Firestore returns no events.
 */
public class DiscoverFragment extends Fragment {

    private EventBigAdapter   mSuggestedAdapter;
    private EventSmallAdapter mThisWeekAdapter;
    private RecyclerView      mRvSuggested;
    private RecyclerView      mRvThisWeek;
    private TextView          mTvEmptySuggested;
    private TextView          mTvEmptyThisWeek;

    private final RsvpHandler  mRsvpHandler  = new RsvpHandler();
    private final EventService mEventService = new EventService();
    private ListenerRegistration mUpcomingListener;
    private ListenerRegistration mThisWeekListener;
    private ListenerRegistration mRsvpHistoryListener;

    private List<Event> mAllUpcoming  = new ArrayList<>();
    private List<Event> mRsvpHistory  = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRvSuggested      = view.findViewById(R.id.recyclerViewSuggested);
        mRvThisWeek       = view.findViewById(R.id.recyclerViewThisWeek);
        mTvEmptySuggested = view.findViewById(R.id.tvEmptySuggested);
        mTvEmptyThisWeek  = view.findViewById(R.id.tvEmptyThisWeek);

        // ── Suggested — horizontal carousel ──────────────────────────────
        mRvSuggested.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        mSuggestedAdapter = new EventBigAdapter(requireContext(), new ArrayList<>(),
                (event, pos) -> mRsvpHandler.toggle(event,
                        () -> mSuggestedAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()),
                this::onEventClick,
                event -> {
                    new DismissedEventsStore(requireContext()).dismiss(event.getId());
                    refreshSuggested();
                });
        mRvSuggested.setAdapter(mSuggestedAdapter);

        // ── This Week — vertical list ─────────────────────────────────────
        mRvThisWeek.setLayoutManager(new LinearLayoutManager(requireContext()));
        mThisWeekAdapter = new EventSmallAdapter(requireContext(), new ArrayList<>(),
                (event, pos) -> mRsvpHandler.toggle(event,
                        () -> mThisWeekAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()),
                this::onEventClick);
        mRvThisWeek.setAdapter(mThisWeekAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mUpcomingListener = mEventService.listenUpcomingEvents(
                events -> {
                    mAllUpcoming = events;
                    refreshSuggested();
                },
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());

        String uid = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : "";
        mRsvpHistoryListener = mEventService.listenMyRsvpedEvents(uid,
                events -> {
                    mRsvpHistory = events;
                    refreshSuggested();
                },
                err -> { /* non-critical — recommendations still work without history */ });

        mThisWeekListener = mEventService.listenThisWeekEvents(
                events -> updateThisWeek(events),
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUpcomingListener != null) {
            mUpcomingListener.remove();
            mUpcomingListener = null;
        }
        if (mRsvpHistoryListener != null) {
            mRsvpHistoryListener.remove();
            mRsvpHistoryListener = null;
        }
        if (mThisWeekListener != null) {
            mThisWeekListener.remove();
            mThisWeekListener = null;
        }
    }

    /** Runs the recommendation engine and refreshes the For You carousel. */
    private void refreshSuggested() {
        if (mSuggestedAdapter == null) return;
        String uid = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : "";
        DismissedEventsStore store = new DismissedEventsStore(requireContext());
        List<Event> recommendations = RecommendationEngine.recommend(
                mRsvpHistory, mAllUpcoming, store.getDismissedIds(), uid);
        mSuggestedAdapter.updateData(recommendations);
        boolean empty = recommendations.isEmpty();
        mRvSuggested.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvEmptySuggested.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void onEventClick(Event event) {
        if (event.getId() == null) return;
        Intent intent = new Intent(requireActivity(), EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    /** Updates the This Week list and toggles its empty-state view. */
    private void updateThisWeek(List<Event> events) {
        mThisWeekAdapter.updateData(events);
        boolean empty = events.isEmpty();
        mRvThisWeek.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvEmptyThisWeek.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
