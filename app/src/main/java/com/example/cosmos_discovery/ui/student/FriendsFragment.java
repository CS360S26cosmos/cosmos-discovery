package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.cosmos_discovery.adapter.FriendAdapter;
import com.example.cosmos_discovery.adapter.FriendEventAdapter;
import com.example.cosmos_discovery.ui.organizer.EventDetailsActivity;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FriendEntry;
import com.example.cosmos_discovery.util.FriendEventFilter;
import com.example.cosmos_discovery.util.RoleUtil;
import com.example.cosmos_discovery.util.RsvpHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Friends tab: horizontal carousel of accepted friends + vertical feed of events
 * that at least one friend has RSVP'd to.
 *
 * Data is refreshed by polling Firestore every {@link #POLL_INTERVAL_MS} milliseconds.
 * Polling starts in {@link #onStart()} and stops in {@link #onStop()}.
 */
public class FriendsFragment extends Fragment
        implements FriendEventAdapter.OnRsvpClickListener {

    private static final long POLL_INTERVAL_MS = 30_000L;

    private RecyclerView       mRvFriends;
    private RecyclerView       mRvFriendEvents;
    private TextView           mTvNoFriends;
    private TextView           mTvNoFriendEvents;

    private FriendAdapter      mFriendAdapter;
    private FriendEventAdapter mFriendEventAdapter;

    private final FriendService mFriendService = new FriendService();
    private final EventService  mEventService  = new EventService();
    private final RsvpHandler   mRsvpHandler   = new RsvpHandler();

    private final Handler mPollHandler = new Handler(Looper.getMainLooper());
    private Runnable      mPollRunnable;

    // Latest data from each fetch — recombined whenever either updates
    private List<FriendEntry> mFriendEntries = new ArrayList<>();
    private List<Event>       mAllEvents     = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRvFriends        = view.findViewById(R.id.recyclerViewFriends);
        mRvFriendEvents   = view.findViewById(R.id.recyclerViewFriendEvents);
        mTvNoFriends      = view.findViewById(R.id.tvNoFriends);
        mTvNoFriendEvents = view.findViewById(R.id.tvNoFriendEvents);

        // Friends carousel (horizontal)
        mFriendAdapter = new FriendAdapter(new ArrayList<>(), entry -> {
            Intent intent = new Intent(requireActivity(), UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_USER_ID, entry.getUid());
            startActivity(intent);
        });
        mRvFriends.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        mRvFriends.setAdapter(mFriendAdapter);

        // Friend events list (vertical)
        mFriendEventAdapter = new FriendEventAdapter(
                requireContext(), new ArrayList<>(), new HashMap<>(), this, this::onEventClick);
        mRvFriendEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvFriendEvents.setAdapter(mFriendEventAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid() : null;
        if (uid == null) return;

        mPollRunnable = new Runnable() {
            @Override
            public void run() {
                fetchAndUpdate(uid);
                mPollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
        mPollHandler.post(mPollRunnable); // fire immediately, then every 30 s
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPollRunnable != null) {
            mPollHandler.removeCallbacks(mPollRunnable);
            mPollRunnable = null;
        }
    }

    /** Fetches friends and upcoming events in parallel, then redraws the feed. */
    private void fetchAndUpdate(String uid) {
        mFriendService.fetchFriends(uid,
                entries -> {
                    mFriendEntries = entries;
                    mFriendAdapter.updateData(entries);
                    updateFriendEmptyState();
                    updateFriendEvents();
                },
                err -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                });

        mEventService.fetchUpcomingEvents(
                events -> {
                    mAllEvents = events;
                    updateFriendEvents();
                },
                err -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Filters {@code mAllEvents} to those attended by at least one friend,
     * then updates the FriendEventAdapter with the filtered list and name map.
     */
    private void updateFriendEvents() {
        Map<String, String> friendUidToName = new HashMap<>();
        Set<String>         friendUids      = new HashSet<>();
        for (FriendEntry entry : mFriendEntries) {
            friendUids.add(entry.getUid());
            friendUidToName.put(entry.getUid(), entry.getName());
        }

        List<Event> filtered = FriendEventFilter.filterByFriends(mAllEvents, friendUids);
        mFriendEventAdapter.updateData(filtered, friendUidToName);
        updateEventEmptyState(filtered.isEmpty());
    }

    /** Toggles the friends carousel and its empty-state view based on {@code mFriendEntries}. */
    private void updateFriendEmptyState() {
        boolean empty = mFriendEntries.isEmpty();
        mRvFriends.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvNoFriends.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /** Toggles the friend-events list and its empty-state view. */
    private void updateEventEmptyState(boolean empty) {
        mRvFriendEvents.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvNoFriendEvents.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void onEventClick(Event event) {
        if (event.getId() == null) return;
        Intent intent = new Intent(requireActivity(), EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    // ── FriendEventAdapter.OnRsvpClickListener ───────────────────────────

    @Override
    public void onRsvpClick(Event event, int position) {
        mRsvpHandler.toggle(event,
                () -> mFriendEventAdapter.notifyItemChanged(position),
                err -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                });
    }
}
