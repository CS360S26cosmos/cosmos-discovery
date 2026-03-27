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
import com.example.cosmos_discovery.adapter.UserSearchAdapter;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.FriendEntry;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment shown when the user taps the search bar on the Friends tab.
 * Fetches all users once and filters them client-side by query.
 * "Add Friend" auto-accepts (writes both sides) with no pending state.
 */
public class UserSearchFragment extends Fragment {

    private RecyclerView      mRecyclerView;
    private TextView          mTvNoResults;
    private UserSearchAdapter mAdapter;

    private final FriendService mFriendService = new FriendService();
    private ListenerRegistration mFriendsListener;

    private List<User>   mAllUsers   = new ArrayList<>();
    private Set<String>  mFriendUids = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.recyclerViewUserSearch);
        mTvNoResults  = view.findViewById(R.id.tvNoResults);

        User currentUser = RoleUtil.getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";

        mAdapter = new UserSearchAdapter(
                requireContext(), currentUid, mAllUsers, mFriendUids,
                this::onAddFriendClick);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setAdapter(mAdapter);

        // Load all users once
        mFriendService.fetchAllUsers(
                users -> {
                    mAllUsers = users;
                    mAdapter.updateData(mAllUsers, mFriendUids);
                    updateEmptyState();
                },
                err -> Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show());

        // Listen to current user's friends so the button state stays up to date
        if (currentUid.isEmpty()) return;
        mFriendsListener = mFriendService.listenFriends(currentUid,
                entries -> {
                    mFriendUids = new HashSet<>();
                    for (FriendEntry e : entries) mFriendUids.add(e.getUid());
                    mAdapter.updateFriendUids(mFriendUids);
                    updateEmptyState();
                },
                err -> { /* non-critical — button state may lag */ });
    }

    /** Called by StudentActivity whenever the search query changes. */
    public void updateQuery(String query) {
        if (mAdapter != null) {
            mAdapter.setQuery(query);
            updateEmptyState();
        }
    }

    private void onAddFriendClick(User target, int position) {
        User currentUser = RoleUtil.getCurrentUser();
        if (currentUser == null) return;

        // Optimistic update: add to local set and refresh adapter
        mFriendUids.add(target.getUid());
        mAdapter.updateFriendUids(mFriendUids);

        mFriendService.addFriend(currentUser, target,
                () -> { /* listener will refresh the button state */ },
                err -> {
                    // Roll back optimistic update on failure
                    mFriendUids.remove(target.getUid());
                    mAdapter.updateFriendUids(mFriendUids);
                    Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (mAdapter == null) return;
        boolean empty = mAdapter.getItemCount() == 0;
        mTvNoResults.setVisibility(empty ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFriendsListener != null) {
            mFriendsListener.remove();
            mFriendsListener = null;
        }
    }
}
