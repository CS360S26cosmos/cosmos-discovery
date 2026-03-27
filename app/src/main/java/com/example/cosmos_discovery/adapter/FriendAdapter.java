package com.example.cosmos_discovery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.FriendEntry;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Horizontal RecyclerView adapter for the friends carousel on the Friends tab.
 * Inflates {@code component_friend.xml} for each entry.
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<FriendEntry> mFriends;

    public FriendAdapter(List<FriendEntry> friends) {
        this.mFriends = friends;
    }

    public void updateData(List<FriendEntry> friends) {
        mFriends = friends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendEntry entry = mFriends.get(position);

        holder.tvFriendName.setText(entry.getName() != null ? entry.getName().split(" ")[0] : "");

        if (entry.getPhotoUrl() != null && !entry.getPhotoUrl().isEmpty()) {
            Glide.with(holder.ivFriendProfile.getContext())
                    .load(entry.getPhotoUrl())
                    .placeholder(R.drawable.ic_topbar_person_icon)
                    .centerCrop()
                    .into(holder.ivFriendProfile);
        } else {
            holder.ivFriendProfile.setImageResource(R.drawable.ic_topbar_person_icon);
        }
    }

    @Override
    public int getItemCount() {
        return mFriends == null ? 0 : mFriends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivFriendProfile;
        final TextView           tvFriendName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFriendProfile = itemView.findViewById(R.id.ivFriendProfile);
            tvFriendName    = itemView.findViewById(R.id.tvFriendName);
        }
    }
}
