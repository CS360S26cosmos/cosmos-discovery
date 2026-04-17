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
import com.example.cosmos_discovery.adapter.EventSmallAdapter;
import com.example.cosmos_discovery.ui.organizer.EventDetailsActivity;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FilterState;
import com.example.cosmos_discovery.util.EventFilter;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Search screen — displays filtered results as the user types in the header EditText.
 *
 * StudentActivity owns the EditText and calls {@link #updateQuery(String)} on every
 * text change. This fragment holds the full event list and re-filters it client-side
 * on each update, matching against title, location, and tags.
 */
public class SearchFragment extends Fragment {

    private final EventService   mEventService  = new EventService();
    private final RsvpHandler    mRsvpHandler   = new RsvpHandler();
    private EventSmallAdapter    mAdapter;
    private ListenerRegistration mEventsListener;

    private List<Event> mAllEvents     = new ArrayList<>();
    private String      mCurrentQuery  = "";
    private FilterState mCurrentFilter = new FilterState();

    private RecyclerView mRecyclerView;
    private TextView     mEmptyText;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mEmptyText    = view.findViewById(R.id.textSearchEmpty);
        mRecyclerView = view.findViewById(R.id.recyclerViewSearch);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mAdapter = new EventSmallAdapter(requireContext(), new ArrayList<>(),
                (event, pos) -> mRsvpHandler.toggle(event,
                        () -> mAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()),
                this::onEventClick);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mEventsListener = mEventService.listenUpcomingEvents(
                events -> {
                    mAllEvents = events;
                    applyFilter();
                },
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mEventsListener != null) {
            mEventsListener.remove();
            mEventsListener = null;
        }
    }

    private void onEventClick(Event event) {
        if (event.getId() == null) return;
        Intent intent = new Intent(requireActivity(), EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    // ── Public API (called by StudentActivity) ────────────────────────────

    /** Called by StudentActivity on every keystroke in the search EditText. */
    public void updateQuery(String query) {
        mCurrentQuery = query == null ? "" : query;
        applyFilter();
    }

    /** Called by StudentActivity when the user taps "View Results" or "Clear Filters". */
    public void updateFilters(FilterState filter) {
        mCurrentFilter = filter != null ? filter : new FilterState();
        applyFilter();
    }

    // ── Filter logic ──────────────────────────────────────────────────────

    /**
     * Applies the current query and filter state client-side against {@code mAllEvents}.
     * Updates the adapter and toggles the empty-state view accordingly.
     * A no-op if the fragment is not yet attached to its activity.
     */
    private void applyFilter() {
        if (!isAdded()) return;

        boolean hasQuery  = !mCurrentQuery.trim().isEmpty();
        boolean hasFilter = mCurrentFilter.hasActiveFilters();

        if (!hasQuery && !hasFilter) {
            mAdapter.updateData(new ArrayList<>());
            showEmptyState("Start typing or apply filters to search events");
            return;
        }

        List<Event> results = hasQuery
                ? EventFilter.textSearch(mAllEvents, mCurrentQuery)
                : new ArrayList<>(mAllEvents);
        results = EventFilter.applyFilters(results, mCurrentFilter);

        mAdapter.updateData(results);
        if (results.isEmpty()) {
            String msg = hasQuery
                    ? "No events found for \"" + mCurrentQuery.trim() + "\""
                    : "No events match the selected filters";
            showEmptyState(msg);
        } else {
            hideEmptyState();
        }
    }

    // ── Empty state helpers ───────────────────────────────────────────────

    /** Shows the empty-state text with {@code message} and hides the RecyclerView. */
    private void showEmptyState(String message) {
        mEmptyText.setText(message);
        mEmptyText.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    /** Hides the empty-state text and shows the RecyclerView. */
    private void hideEmptyState() {
        mEmptyText.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }
}
