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
import com.example.cosmos_discovery.model.OrganizerRequest;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the admin queue of pending organizer-role requests.
 *
 * <p>Renders one row per {@link OrganizerRequest} with Approve/Reject actions
 * dispatched to {@link OnRequestActionListener}.
 */
public class OrganizerRequestAdapter
        extends RecyclerView.Adapter<OrganizerRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(OrganizerRequest request);
        void onReject(OrganizerRequest request);
    }

    private List<OrganizerRequest> mData = new ArrayList<>();
    private final OnRequestActionListener mListener;

    public OrganizerRequestAdapter(OnRequestActionListener listener) {
        mListener = listener;
    }

    public void updateData(List<OrganizerRequest> data) {
        mData = data != null ? new ArrayList<>(data) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(OrganizerRequest request) {
        int idx = -1;
        for (int i = 0; i < mData.size(); i++) {
            if (request.getUserId() != null
                    && request.getUserId().equals(mData.get(i).getUserId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            mData.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        OrganizerRequest req = mData.get(position);

        h.tvName.setText(req.getUserName() != null ? req.getUserName() : "");
        h.tvEmail.setText(req.getUserEmail() != null ? req.getUserEmail() : "");

        if (req.getUserPhotoUrl() != null && !req.getUserPhotoUrl().isEmpty()) {
            Glide.with(h.ivPhoto.getContext())
                    .load(req.getUserPhotoUrl())
                    .placeholder(R.drawable.ic_topbar_person_icon)
                    .centerCrop()
                    .into(h.ivPhoto);
        } else {
            h.ivPhoto.setImageResource(R.drawable.ic_topbar_person_icon);
        }

        h.btnAccept.setOnClickListener(v -> {
            if (mListener != null) mListener.onAccept(req);
        });
        h.btnReject.setOnClickListener(v -> {
            if (mListener != null) mListener.onReject(req);
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivPhoto;
        final TextView tvName;
        final TextView tvEmail;
        final Button btnAccept;
        final Button btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto   = itemView.findViewById(R.id.ivRequestPhoto);
            tvName    = itemView.findViewById(R.id.tvRequestName);
            tvEmail   = itemView.findViewById(R.id.tvRequestEmail);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
