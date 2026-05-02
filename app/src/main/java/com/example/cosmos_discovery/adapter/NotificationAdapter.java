package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Notification;
import com.example.cosmos_discovery.ui.organizer.EventDetailsActivity;
import com.example.cosmos_discovery.ui.student.FriendRequestsActivity;
import com.example.cosmos_discovery.util.TimeAgoUtil;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context mContext;
    private final List<Notification> mItems = new ArrayList<>();

    public NotificationAdapter(Context context) {
        mContext = context;
    }

    public void updateData(List<Notification> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mItems.get(position));
    }

    @Override
    public int getItemCount() { return mItems.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView  tvTitle;
        final TextView  tvMessage;
        final TextView  tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon    = itemView.findViewById(R.id.ivIcon);
            tvTitle   = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
        }

        void bind(Notification n) {
            tvTitle.setText(n.getTitle());
            tvMessage.setText(n.getMessage());
            tvTime.setText(TimeAgoUtil.format(n.getTimestamp()));

            switch (n.getType() != null ? n.getType() : "") {
                case Notification.TYPE_FRIEND_REQUEST_RECEIVED:
                case Notification.TYPE_FRIEND_REQUEST_ACCEPTED:
                    ivIcon.setImageResource(R.drawable.ic_group);
                    break;
                case Notification.TYPE_RSVP_CONFIRMED:
                    ivIcon.setImageResource(R.drawable.ic_check_circle);
                    break;
                case Notification.TYPE_EVENT_UPDATED:
                    ivIcon.setImageResource(R.drawable.ic_eventdetails_calendar);
                    break;
                case Notification.TYPE_EVENT_CANCELLED:
                    ivIcon.setImageResource(R.drawable.ic_cancel);
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_notifications);
                    break;
            }

            if (Notification.TYPE_EVENT_UPDATED.equals(n.getType()) && n.getEventId() != null) {
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(mContext, EventDetailsActivity.class);
                    intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, n.getEventId());
                    mContext.startActivity(intent);
                });
            } else if (Notification.TYPE_FRIEND_REQUEST_RECEIVED.equals(n.getType())) {
                itemView.setOnClickListener(v ->
                        mContext.startActivity(new Intent(mContext, FriendRequestsActivity.class)));
            } else {
                itemView.setOnClickListener(null);
            }
        }
    }
}
