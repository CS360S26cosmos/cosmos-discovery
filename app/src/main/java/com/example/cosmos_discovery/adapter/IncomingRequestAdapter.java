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
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * RecyclerView adapter for incoming friend requests.
 *
 * <p>Renders one row per pending request with Accept/Decline actions
 * dispatched to {@link OnActionListener}.
 */
public class IncomingRequestAdapter
        extends RecyclerView.Adapter<IncomingRequestAdapter.ViewHolder> {

    public interface OnActionListener {
        void onAccept(RequestItem item);
        void onDecline(RequestItem item);
    }

    public static class RequestItem {
        public final String uid;       // sender's UID — needed for accept/decline calls
        public final String name;
        public final String subtitle;
        public final String initials;
        public final String photoUrl;

        public RequestItem(String uid, String name, String subtitle,
                           String initials, String photoUrl) {
            this.uid      = uid;
            this.name     = name;
            this.subtitle = subtitle;
            this.initials = initials;
            this.photoUrl = photoUrl;
        }
    }

    private List<RequestItem> mItems;
    private final OnActionListener mListener;

    public IncomingRequestAdapter(List<RequestItem> items, OnActionListener listener) {
        mItems   = items;
        mListener = listener;
    }

    public void updateData(List<RequestItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incoming_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        RequestItem item = mItems.get(position);
        h.tvName.setText(item.name != null ? item.name : "");
        h.tvSubtitle.setText(item.subtitle != null ? item.subtitle : "");

        // Show initials; load photo over them if available
        h.tvInitials.setText(item.initials != null ? item.initials : "");
        if (item.photoUrl != null && !item.photoUrl.isEmpty()) {
            h.tvInitials.setVisibility(View.GONE);
            Glide.with(h.ivAvatar.getContext())
                    .load(item.photoUrl)
                    .centerCrop()
                    .into(h.ivAvatar);
        } else {
            h.tvInitials.setVisibility(View.VISIBLE);
            h.ivAvatar.setImageDrawable(null);
        }

        h.btnAccept.setOnClickListener(v -> {
            if (mListener != null) mListener.onAccept(item);
        });
        h.btnDecline.setOnClickListener(v -> {
            if (mListener != null) mListener.onDecline(item);
        });
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivAvatar;
        final TextView tvInitials;
        final TextView tvName;
        final TextView tvSubtitle;
        final Button btnAccept;
        final Button btnDecline;

        ViewHolder(@NonNull View v) {
            super(v);
            ivAvatar   = v.findViewById(R.id.ivAvatar);
            tvInitials = v.findViewById(R.id.tvInitials);
            tvName     = v.findViewById(R.id.tvName);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            btnAccept  = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
        }
    }
}
