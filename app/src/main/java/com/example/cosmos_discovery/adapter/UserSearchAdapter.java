package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Vertical RecyclerView adapter for the user search screen.
 * Filters {@code allUsers} client-side by the current query (case-insensitive name match).
 * Excludes the current user and shows "Add Friend" vs "Friends ✓" based on {@code friendUids}.
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    public interface OnAddFriendListener {
        void onAddFriend(User user, int position);
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private List<User>              mAllUsers;
    private List<User>              mFiltered;
    private Set<String>             mFriendUids;
    private String                  mCurrentUid;
    private String                  mQuery      = "";
    private final Context           mContext;
    private final OnAddFriendListener mListener;
    private final OnUserClickListener mUserClickListener;

    public UserSearchAdapter(Context context, String currentUid,
                             List<User> allUsers, Set<String> friendUids,
                             OnAddFriendListener listener,
                             OnUserClickListener userClickListener) {
        this.mContext            = context;
        this.mCurrentUid         = currentUid;
        this.mAllUsers           = allUsers;
        this.mFriendUids         = friendUids;
        this.mListener           = listener;
        this.mUserClickListener  = userClickListener;
        this.mFiltered           = new ArrayList<>();
    }

    /** Updates both the user list and current friend UIDs, then re-filters. */
    public void updateData(List<User> allUsers, Set<String> friendUids) {
        mAllUsers   = allUsers;
        mFriendUids = friendUids;
        applyFilter();
    }

    /** Updates only the friend UIDs set (e.g. after a successful addFriend), then re-filters. */
    public void updateFriendUids(Set<String> friendUids) {
        mFriendUids = friendUids;
        applyFilter();
    }

    /** Sets the search query and re-filters the list. */
    public void setQuery(String query) {
        mQuery = query == null ? "" : query;
        applyFilter();
    }

    private void applyFilter() {
        mFiltered = new ArrayList<>();
        String lower = mQuery.toLowerCase(java.util.Locale.getDefault());
        for (User u : mAllUsers) {
            if (u.getUid() == null || u.getUid().equals(mCurrentUid)) continue;
            if (lower.isEmpty() || (u.getName() != null
                    && u.getName().toLowerCase(java.util.Locale.getDefault()).contains(lower))) {
                mFiltered.add(u);
            }
        }
        notifyDataSetChanged();
    }

    public List<User> getFilteredList() {
        return mFiltered;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = mFiltered.get(position);

        holder.tvUserName.setText(user.getName() != null ? user.getName() : "");
        holder.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // Profile photo
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(holder.ivUserPhoto.getContext())
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_topbar_person_icon)
                    .centerCrop()
                    .into(holder.ivUserPhoto);
        } else {
            holder.ivUserPhoto.setImageResource(R.drawable.ic_topbar_person_icon);
        }

        // Whole-row click → open user profile
        holder.itemView.setOnClickListener(v -> {
            if (mUserClickListener != null) mUserClickListener.onUserClick(user);
        });

        // Add Friend / Friends state
        boolean isFriend = mFriendUids != null && mFriendUids.contains(user.getUid());
        if (isFriend) {
            holder.btnAddFriend.setText("Friends ✓");
            holder.btnAddFriend.setBackground(
                    ContextCompat.getDrawable(mContext, R.drawable.bg_btn_going));
            holder.btnAddFriend.setTextColor(
                    ContextCompat.getColor(mContext, R.color.color_button_going_stroke));
            holder.btnAddFriend.setEnabled(false);
        } else {
            holder.btnAddFriend.setText("Add Friend");
            holder.btnAddFriend.setBackground(
                    ContextCompat.getDrawable(mContext, R.drawable.bg_btn_rsvp));
            holder.btnAddFriend.setTextColor(Color.WHITE);
            holder.btnAddFriend.setEnabled(true);
            holder.btnAddFriend.setOnClickListener(v -> {
                int p = holder.getAdapterPosition();
                if (p != RecyclerView.NO_ID) mListener.onAddFriend(user, p);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mFiltered == null ? 0 : mFiltered.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivUserPhoto;
        final TextView           tvUserName;
        final TextView           tvUserEmail;
        final Button             btnAddFriend;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserPhoto  = itemView.findViewById(R.id.ivUserPhoto);
            tvUserName   = itemView.findViewById(R.id.tvUserName);
            tvUserEmail  = itemView.findViewById(R.id.tvUserEmail);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }
    }
}
