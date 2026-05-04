package com.example.cosmos_discovery.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Announcement;
import com.example.cosmos_discovery.util.TimeAgoUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the announcement feed shown to event attendees.
 *
 * <p>Each row renders an {@link Announcement} with its body and a relative
 * "time ago" timestamp produced by {@link TimeAgoUtil}.
 */
public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {

    private final List<Announcement> mItems = new ArrayList<>();

    public void updateData(List<Announcement> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Announcement a = mItems.get(position);
        holder.message.setText(a.getMessage());
        holder.time.setText(TimeAgoUtil.format(a.getSentAt()));
    }

    @Override
    public int getItemCount() { return mItems.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView time;
        VH(@NonNull View v) {
            super(v);
            message = v.findViewById(R.id.tvAnnouncementMessage);
            time    = v.findViewById(R.id.tvAnnouncementTime);
        }
    }
}
