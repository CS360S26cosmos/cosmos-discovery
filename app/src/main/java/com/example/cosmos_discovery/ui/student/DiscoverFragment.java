package com.example.cosmos_discovery.ui.student;

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
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
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
        mSuggestedAdapter = new EventBigAdapter(requireContext(), new ArrayList<>(), (event, pos) ->
                mRsvpHandler.toggle(event,
                        () -> mSuggestedAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()));
        mRvSuggested.setAdapter(mSuggestedAdapter);

        // ── This Week — vertical list ─────────────────────────────────────
        mRvThisWeek.setLayoutManager(new LinearLayoutManager(requireContext()));
        mThisWeekAdapter = new EventSmallAdapter(requireContext(), new ArrayList<>(), (event, pos) ->
                mRsvpHandler.toggle(event,
                        () -> mThisWeekAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()));
        mRvThisWeek.setAdapter(mThisWeekAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mUpcomingListener = mEventService.listenUpcomingEvents(
                events -> updateSuggested(events),
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());

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
        if (mThisWeekListener != null) {
            mThisWeekListener.remove();
            mThisWeekListener = null;
        }
    }

    /** Updates the Suggested carousel and toggles its empty-state view. */
    private void updateSuggested(List<Event> events) {
        mSuggestedAdapter.updateData(events);
        boolean empty = events.isEmpty();
        mRvSuggested.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvEmptySuggested.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /** Updates the This Week list and toggles its empty-state view. */
    private void updateThisWeek(List<Event> events) {
        mThisWeekAdapter.updateData(events);
        boolean empty = events.isEmpty();
        mRvThisWeek.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvEmptyThisWeek.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
