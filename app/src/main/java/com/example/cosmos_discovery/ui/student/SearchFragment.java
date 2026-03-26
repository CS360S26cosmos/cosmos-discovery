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
import com.example.cosmos_discovery.adapter.EventSmallAdapter;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FilterState;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        mAdapter = new EventSmallAdapter(requireContext(), new ArrayList<>(), (event, pos) ->
                mRsvpHandler.toggle(event,
                        () -> mAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()));
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

    // ── Search logic ──────────────────────────────────────────────────────

    private void applyFilter() {
        if (!isAdded()) return;

        boolean hasQuery  = !mCurrentQuery.trim().isEmpty();
        boolean hasFilter = mCurrentFilter.hasActiveFilters();

        if (!hasQuery && !hasFilter) {
            mAdapter.updateData(new ArrayList<>());
            showEmptyState("Start typing or apply filters to search events");
            return;
        }

        // Step 1: start with all events; apply text filter first if there's a query
        List<Event> results = hasQuery
                ? filter(mAllEvents, mCurrentQuery)
                : new ArrayList<>(mAllEvents);

        // Step 2: category — keep events whose tags contain the selected category
        String selectedCategory = mCurrentFilter.getSelectedCategory();
        if (selectedCategory != null) {
            results.removeIf(e -> {
                List<String> tags = e.getTags();
                return tags == null || !tags.contains(selectedCategory);
            });
        }

        // Step 3: date range — keep events whose dateTime falls within [start, end]
        long[] range = mCurrentFilter.getDateRange();
        if (range != null) {
            results.removeIf(e -> e.getDateTime() < range[0] || e.getDateTime() > range[1]);
        }

        // Step 4: access type
        FilterState.AccessFilter access = mCurrentFilter.getSelectedAccess();
        if (access == FilterState.AccessFilter.LUMS_ONLY) {
            results.removeIf(e -> !"lums_only".equals(e.getAccessType()));
        } else if (access == FilterState.AccessFilter.OPEN) {
            results.removeIf(e -> e.getAccessType() != null && !"open".equals(e.getAccessType()));
        }

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

    /**
     * All-tokens-must-appear fuzzy filter across title, location, and tags.
     *
     * Each space-separated token in the query must be contained somewhere in
     * the combined searchable string — enabling partial matches ("mus" → "music")
     * and cross-field queries ("music auditorium" → title + location).
     */
    private List<Event> filter(List<Event> events, String rawQuery) {
        String query  = rawQuery.toLowerCase(Locale.getDefault()).trim();
        String[] tokens = query.split("\\s+");
        List<Event> results = new ArrayList<>();

        for (Event event : events) {
            String searchable = buildSearchable(event);
            boolean allMatch = true;
            for (String token : tokens) {
                if (!searchable.contains(token)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) results.add(event);
        }
        return results;
    }

    private String buildSearchable(Event event) {
        List<String> tags = event.getTags();
        String tagText = (tags != null) ? String.join(" ", tags) : "";
        return (event.getTitle() + " " + event.getLocation() + " " + tagText)
                .toLowerCase(Locale.getDefault());
    }

    // ── Empty state helpers ───────────────────────────────────────────────

    private void showEmptyState(String message) {
        mEmptyText.setText(message);
        mEmptyText.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        mEmptyText.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }
}
