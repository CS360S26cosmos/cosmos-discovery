package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

/**
 * Discover screen for the student role.
 *
 * Two sections:
 *   • "Suggested"  — horizontal carousel of big cards ({@code recyclerViewSuggested})
 *   • "This Week"  — vertical list of small cards  ({@code recyclerViewThisWeek})
 *
 * Both sections are driven by the same upcoming-events Firestore listener for now.
 */
public class DiscoverFragment extends Fragment {

    private EventBigAdapter      mSuggestedAdapter;
    private EventSmallAdapter    mThisWeekAdapter;
    private final RsvpHandler    mRsvpHandler  = new RsvpHandler();
    private final EventService   mEventService = new EventService();
    private ListenerRegistration mEventsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // ── Suggested — horizontal carousel ──────────────────────────────
        RecyclerView rvSuggested = view.findViewById(R.id.recyclerViewSuggested);
        rvSuggested.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        mSuggestedAdapter = new EventBigAdapter(requireContext(), new ArrayList<>(), (event, pos) ->
                mRsvpHandler.toggle(event,
                        () -> mSuggestedAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()));
        rvSuggested.setAdapter(mSuggestedAdapter);

        // ── This Week — vertical list ─────────────────────────────────────
        RecyclerView rvThisWeek = view.findViewById(R.id.recyclerViewThisWeek);
        rvThisWeek.setLayoutManager(new LinearLayoutManager(requireContext()));

        mThisWeekAdapter = new EventSmallAdapter(requireContext(), new ArrayList<>(), (event, pos) ->
                mRsvpHandler.toggle(event,
                        () -> mThisWeekAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()));
        rvThisWeek.setAdapter(mThisWeekAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mEventsListener = mEventService.listenUpcomingEvents(
                events -> {
                    mSuggestedAdapter.updateData(events);
                    mThisWeekAdapter.updateData(events);
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
}
