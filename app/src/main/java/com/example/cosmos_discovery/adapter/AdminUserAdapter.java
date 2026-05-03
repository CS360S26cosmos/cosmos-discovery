package com.example.cosmos_discovery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the admin "Users" tab.
 *
 * <p>Renders one row per {@link User} with controls to deactivate or reactivate
 * the account, surfaced through {@link OnUserActionListener}.
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    public interface OnUserActionListener {
        void onDeactivate(User user);
        void onReactivate(User user);
        void onChangeStatus(User user);
    }

    private List<User> mAllUsers  = new ArrayList<>();
    private List<User> mFiltered  = new ArrayList<>();
    private String     mQuery     = "";
    private String     mCurrentAdminUid;
    private final OnUserActionListener mListener;

    public AdminUserAdapter(String currentAdminUid, OnUserActionListener listener) {
        mCurrentAdminUid = currentAdminUid;
        mListener = listener;
    }

    public void updateData(List<User> users) {
        mAllUsers = users != null ? new ArrayList<>(users) : new ArrayList<>();
        applyFilter();
    }

    public void setQuery(String query) {
        mQuery = query == null ? "" : query;
        applyFilter();
    }

    /** Update a single user entry in place (e.g. after role or active change). */
    public void updateUser(User updated) {
        for (int i = 0; i < mAllUsers.size(); i++) {
            if (updated.getUid() != null && updated.getUid().equals(mAllUsers.get(i).getUid())) {
                mAllUsers.set(i, updated);
                break;
            }
        }
        applyFilter();
    }

    private void applyFilter() {
        mFiltered = new ArrayList<>();
        String lower = mQuery.toLowerCase(Locale.getDefault());
        for (User u : mAllUsers) {
            if (lower.isEmpty()) {
                mFiltered.add(u);
            } else {
                boolean nameMatch = u.getName() != null
                        && u.getName().toLowerCase(Locale.getDefault()).contains(lower);
                boolean emailMatch = u.getEmail() != null
                        && u.getEmail().toLowerCase(Locale.getDefault()).contains(lower);
                if (nameMatch || emailMatch) mFiltered.add(u);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        User user = mFiltered.get(position);

        h.tvName.setText(user.getName() != null ? user.getName() : "");
        h.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // Role badge
        String role = user.getRole();
        String badge;
        if (User.ROLE_ADMIN.equals(role))          badge = "Admin";
        else if (User.ROLE_ORGANIZER.equals(role)) badge = "Organizer";
        else                                        badge = "Student";
        if (!user.isActive()) badge += " · Inactive";
        h.tvRoleBadge.setText(badge);

        // Profile photo
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(h.ivPhoto.getContext())
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_topbar_person_icon)
                    .centerCrop()
                    .into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_topbar_person_icon);
        }

        // Deactivate / Reactivate and Change Status buttons — hidden for current admin (self-protection)
        boolean isCurrentAdmin = user.getUid() != null
                && user.getUid().equals(mCurrentAdminUid);
        if (isCurrentAdmin) {
            h.btnDeactivate.setVisibility(View.GONE);
            h.btnChangeStatus.setVisibility(View.GONE);
        } else {
            h.btnDeactivate.setVisibility(View.VISIBLE);
            h.btnChangeStatus.setVisibility(View.VISIBLE);
            if (user.isActive()) {
                h.btnDeactivate.setText("Deactivate");
                h.btnDeactivate.setOnClickListener(v -> {
                    if (mListener != null) mListener.onDeactivate(user);
                });
            } else {
                h.btnDeactivate.setText("Reactivate");
                h.btnDeactivate.setOnClickListener(v -> {
                    if (mListener != null) mListener.onReactivate(user);
                });
            }
            h.btnChangeStatus.setOnClickListener(v -> {
                if (mListener != null) mListener.onChangeStatus(user);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mFiltered.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivPhoto;
        final TextView tvName;
        final TextView tvEmail;
        final TextView tvRoleBadge;
        final Button   btnDeactivate;
        final Button   btnChangeStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto         = itemView.findViewById(R.id.ivUserPhoto);
            tvName          = itemView.findViewById(R.id.tvUserName);
            tvEmail         = itemView.findViewById(R.id.tvUserEmail);
            tvRoleBadge     = itemView.findViewById(R.id.tvRoleBadge);
            btnDeactivate   = itemView.findViewById(R.id.btnDeactivate);
            btnChangeStatus = itemView.findViewById(R.id.btnChangeStatus);
        }
    }
}
