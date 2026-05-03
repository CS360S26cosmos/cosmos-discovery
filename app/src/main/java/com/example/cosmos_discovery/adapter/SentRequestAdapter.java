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

public class SentRequestAdapter
        extends RecyclerView.Adapter<SentRequestAdapter.ViewHolder> {

    public interface OnCancelListener {
        void onCancel(RequestItem item);
    }

    public static class RequestItem {
        public final String uid;        // target's UID — needed for cancel call
        public final String name;
        public final String statusLine; // e.g. "Pending · Sent 2 days ago"
        public final String initials;
        public final String photoUrl;

        public RequestItem(String uid, String name, String statusLine,
                           String initials, String photoUrl) {
            this.uid        = uid;
            this.name       = name;
            this.statusLine = statusLine;
            this.initials   = initials;
            this.photoUrl   = photoUrl;
        }
    }

    private List<RequestItem> mItems;
    private final OnCancelListener mListener;

    public SentRequestAdapter(List<RequestItem> items, OnCancelListener listener) {
        mItems    = items;
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
                .inflate(R.layout.item_sent_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        RequestItem item = mItems.get(position);
        h.tvName.setText(item.name != null ? item.name : "");
        h.tvStatus.setText(item.statusLine != null ? item.statusLine : "");

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

        h.btnCancel.setOnClickListener(v -> {
            if (mListener != null) mListener.onCancel(item);
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
        final TextView tvStatus;
        final Button btnCancel;

        ViewHolder(@NonNull View v) {
            super(v);
            ivAvatar   = v.findViewById(R.id.ivAvatar);
            tvInitials = v.findViewById(R.id.tvInitials);
            tvName     = v.findViewById(R.id.tvName);
            tvStatus   = v.findViewById(R.id.tvStatus);
            btnCancel  = v.findViewById(R.id.btnCancel);
        }
    }
}
