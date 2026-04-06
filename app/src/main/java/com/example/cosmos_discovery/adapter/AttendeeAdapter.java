package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;

import java.util.List;

/** Simple adapter for rendering event attendees using {@code item_attendee.xml}. */
public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    private final Context   mContext;
    private List<User>      mUsers;

    public AttendeeAdapter(Context context, List<User> users) {
        mContext = context;
        mUsers   = users;
    }

    public void updateData(List<User> users) {
        mUsers = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = mUsers.get(position);

        holder.tvName.setText(user.getName() != null ? user.getName() : "Attendee");
        holder.tvId.setText(user.getUid() != null ? user.getUid() : "");
        holder.tvDept.setText(user.getMajor() != null ? user.getMajor() : "");

        // Stats screen is read-only; hide the "Arrived" status button.
        holder.btnStatus.setVisibility(View.GONE);

        Glide.with(mContext)
                .load(user.getPhotoUrl())
                .placeholder(R.drawable.ic_sidebar_main_profileimage)
                .centerCrop()
                .into(holder.ivProfile);
    }

    @Override
    public int getItemCount() {
        return mUsers == null ? 0 : mUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivProfile;
        final TextView  tvName;
        final TextView  tvId;
        final TextView  tvDept;
        final View      btnStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivAttendeeProfile);
            tvName    = itemView.findViewById(R.id.tvAttendeeName);
            tvId      = itemView.findViewById(R.id.tvAttendeeId);
            tvDept    = itemView.findViewById(R.id.tvAttendeeDept);
            btnStatus = itemView.findViewById(R.id.btnStatus);
        }
    }
}

