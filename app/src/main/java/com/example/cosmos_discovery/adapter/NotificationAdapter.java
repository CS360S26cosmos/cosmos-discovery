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
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public  static final int VIEW_TYPE_HEADER       = 0;
    private static final int VIEW_TYPE_NOTIFICATION = 1;

    private final Context      mContext;
    private final List<Object> mItems = new ArrayList<>();

    public NotificationAdapter(Context context) {
        mContext = context;
    }

    /** Accepts a mixed list of String headers and Notification items. */
    public void updateData(List<Object> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_NOTIFICATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_notification_header, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) mItems.get(position));
        } else {
            ((NotificationViewHolder) holder).bind((Notification) mItems.get(position));
        }
    }

    @Override
    public int getItemCount() { return mItems.size(); }

    /**
     * Returns the Notification at {@code position}, or {@code null} if it's a header.
     * Used by the swipe-to-delete callback.
     */
    public Notification getNotificationAt(int position) {
        Object item = mItems.get(position);
        return item instanceof Notification ? (Notification) item : null;
    }

    // ── Header ViewHolder ─────────────────────────────────────────────────

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvHeader;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
        void bind(String label) { tvHeader.setText(label); }
    }

    // ── Notification ViewHolder ───────────────────────────────────────────

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView  tvTitle;
        final TextView  tvMessage;
        final TextView  tvTime;

        NotificationViewHolder(View itemView) {
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
                case Notification.TYPE_RSVP_RECEIVED:
                case Notification.TYPE_EVENT_APPROVED:
                case Notification.TYPE_CAPACITY_FULL:
                    ivIcon.setImageResource(R.drawable.ic_check_circle);
                    break;
                case Notification.TYPE_EVENT_UPDATED:
                case Notification.TYPE_ANNOUNCEMENT:
                    ivIcon.setImageResource(R.drawable.ic_eventdetails_calendar);
                    break;
                case Notification.TYPE_EVENT_CANCELLED:
                case Notification.TYPE_EVENT_REJECTED:
                    ivIcon.setImageResource(R.drawable.ic_cancel);
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_notifications);
                    break;
            }

            String type = n.getType() != null ? n.getType() : "";
            boolean opensEvent =
                       Notification.TYPE_EVENT_UPDATED.equals(type)
                    || Notification.TYPE_EVENT_CANCELLED.equals(type)
                    || Notification.TYPE_ANNOUNCEMENT.equals(type)
                    || Notification.TYPE_EVENT_APPROVED.equals(type)
                    || Notification.TYPE_EVENT_REJECTED.equals(type)
                    || Notification.TYPE_CAPACITY_FULL.equals(type)
                    || Notification.TYPE_RSVP_CONFIRMED.equals(type)
                    || Notification.TYPE_RSVP_RECEIVED.equals(type);

            if (opensEvent && n.getEventId() != null && !n.getEventId().isEmpty()) {
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(mContext, EventDetailsActivity.class);
                    intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, n.getEventId());
                    mContext.startActivity(intent);
                });
            } else if (Notification.TYPE_FRIEND_REQUEST_RECEIVED.equals(type)
                    || Notification.TYPE_FRIEND_REQUEST_ACCEPTED.equals(type)) {
                itemView.setOnClickListener(v ->
                        mContext.startActivity(new Intent(mContext, FriendRequestsActivity.class)));
            } else {
                itemView.setOnClickListener(null);
            }
        }
    }
}
