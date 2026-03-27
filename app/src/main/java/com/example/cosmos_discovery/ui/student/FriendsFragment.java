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
import com.example.cosmos_discovery.adapter.FriendAdapter;
import com.example.cosmos_discovery.adapter.FriendEventAdapter;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FriendEntry;
import com.example.cosmos_discovery.util.RoleUtil;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

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
 * Two Firestore listeners run in parallel:
 *   1. {@code users/{uid}/friends} subcollection — friend list
 *   2. All upcoming approved events — filtered client-side by friend attendeeIds
 *
 * Both listeners are detached in {@link #onDestroyView()}.
 */
public class FriendsFragment extends Fragment
        implements FriendEventAdapter.OnRsvpClickListener {

    private RecyclerView      mRvFriends;
    private RecyclerView      mRvFriendEvents;
    private TextView          mTvNoFriends;
    private TextView          mTvNoFriendEvents;

    private FriendAdapter     mFriendAdapter;
    private FriendEventAdapter mFriendEventAdapter;

    private final FriendService mFriendService = new FriendService();
    private final EventService  mEventService  = new EventService();
    private final RsvpHandler   mRsvpHandler   = new RsvpHandler();

    private ListenerRegistration mFriendsListener;
    private ListenerRegistration mEventsListener;

    // Latest data from each listener — recombined whenever either updates
    private List<FriendEntry>   mFriendEntries = new ArrayList<>();
    private List<Event>         mAllEvents     = new ArrayList<>();

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
        mFriendAdapter = new FriendAdapter(new ArrayList<>());
        mRvFriends.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        mRvFriends.setAdapter(mFriendAdapter);

        // Friend events list (vertical)
        mFriendEventAdapter = new FriendEventAdapter(
                requireContext(), new ArrayList<>(), new HashMap<>(), this);
        mRvFriendEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvFriendEvents.setAdapter(mFriendEventAdapter);

        String uid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid() : null;
        if (uid == null) return;

        // Listener 1: friends subcollection
        mFriendsListener = mFriendService.listenFriends(uid,
                entries -> {
                    mFriendEntries = entries;
                    mFriendAdapter.updateData(entries);
                    updateFriendEmptyState();
                    updateFriendEvents();
                },
                err -> Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show());

        // Listener 2: all upcoming approved events
        mEventsListener = mEventService.listenUpcomingEvents(
                events -> {
                    mAllEvents = events;
                    updateFriendEvents();
                },
                err -> Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show());
    }

    /**
     * Filters {@code mAllEvents} to those attended by at least one friend,
     * then updates the FriendEventAdapter with the filtered list and name map.
     */
    private void updateFriendEvents() {
        // Build uid → first-name map from current friends
        Map<String, String> friendUidToName = new HashMap<>();
        Set<String>         friendUids      = new HashSet<>();
        for (FriendEntry entry : mFriendEntries) {
            friendUids.add(entry.getUid());
            friendUidToName.put(entry.getUid(), entry.getName());
        }

        // Keep only events where at least one attendee is a friend
        List<Event> filtered = new ArrayList<>();
        for (Event event : mAllEvents) {
            if (event.getAttendeeIds() == null) continue;
            for (String attendeeUid : event.getAttendeeIds()) {
                if (friendUids.contains(attendeeUid)) {
                    filtered.add(event);
                    break;
                }
            }
        }

        mFriendEventAdapter.updateData(filtered, friendUidToName);
        updateEventEmptyState(filtered.isEmpty());
    }

    private void updateFriendEmptyState() {
        boolean empty = mFriendEntries.isEmpty();
        mRvFriends.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvNoFriends.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void updateEventEmptyState(boolean empty) {
        mRvFriendEvents.setVisibility(empty ? View.GONE : View.VISIBLE);
        mTvNoFriendEvents.setVisibility(empty ? View.VISIBLE : View.GONE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFriendsListener != null) { mFriendsListener.remove(); mFriendsListener = null; }
        if (mEventsListener  != null) { mEventsListener.remove();  mEventsListener  = null; }
    }
}
