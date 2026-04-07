package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adapter for rendering event attendees with optional check-in toggle. */
public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    public interface OnCheckInToggleListener {
        void onToggle(String userId, boolean isNowCheckedIn);
    }

    private final Context                mContext;
    private List<User>                   mUsers;
    private Set<String>                  mCheckedInIds;
    private final boolean                mShowCheckIn;
    private final OnCheckInToggleListener mListener;

    /** Full constructor with check-in support. */
    public AttendeeAdapter(Context context, List<User> users, Set<String> checkedInIds,
                           boolean showCheckIn, OnCheckInToggleListener listener) {
        mContext      = context;
        mUsers        = users;
        mCheckedInIds = checkedInIds != null ? checkedInIds : new HashSet<>();
        mShowCheckIn  = showCheckIn;
        mListener     = listener;
    }

    /** Legacy constructor for read-only usage (stats screen). */
    public AttendeeAdapter(Context context, List<User> users) {
        this(context, users, new HashSet<>(), false, null);
    }

    public void updateData(List<User> users, Set<String> checkedInIds) {
        mUsers        = users;
        mCheckedInIds = checkedInIds != null ? checkedInIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void updateData(List<User> users) {
        updateData(users, mCheckedInIds);
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
        holder.tvId.setText(user.getMajor() != null ? user.getMajor() : "");
        holder.tvDept.setText(user.getBatch() != null ? "Batch " + user.getBatch() : "");

        Glide.with(mContext)
                .load(user.getPhotoUrl())
                .placeholder(R.drawable.ic_sidebar_main_profileimage)
                .centerCrop()
                .into(holder.ivProfile);

        if (!mShowCheckIn) {
            holder.btnStatus.setVisibility(View.GONE);
            return;
        }

        holder.btnStatus.setVisibility(View.VISIBLE);
        boolean arrived = mCheckedInIds.contains(user.getUid());
        bindStatusButton((MaterialButton) holder.btnStatus, arrived);

        holder.btnStatus.setOnClickListener(v -> {
            if (mListener != null && user.getUid() != null) {
                mListener.onToggle(user.getUid(), !arrived);
            }
        });
    }

    private void bindStatusButton(MaterialButton btn, boolean arrived) {
        if (arrived) {
            btn.setText("Arrived");
            btn.setIconResource(R.drawable.ic_check_box);
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(mContext, R.color.color_arrived)));
            btn.setTextColor(ContextCompat.getColor(mContext, R.color.white));
            btn.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(mContext, R.color.white)));
        } else {
            btn.setText("Not Arrived");
            btn.setIconResource(R.drawable.ic_check_box_outline);
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(mContext, R.color.color_not_arrived)));
            btn.setTextColor(ContextCompat.getColor(mContext, R.color.white));
            btn.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(mContext, R.color.white)));
        }
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
