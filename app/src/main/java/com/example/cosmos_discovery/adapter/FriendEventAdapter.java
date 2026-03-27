package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.util.RoleUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vertical RecyclerView adapter for the "Events Your Friends Are Attending" feed.
 * Inflates {@code item_event_friends_card.xml}.
 */
public class FriendEventAdapter extends RecyclerView.Adapter<FriendEventAdapter.ViewHolder> {

    public interface OnRsvpClickListener {
        void onRsvpClick(Event event, int position);
    }

    private List<Event>               mEvents;
    private Map<String, String>       mFriendUidToName;
    private final OnRsvpClickListener mListener;
    private final Context             mContext;

    public FriendEventAdapter(Context context, List<Event> events,
                              Map<String, String> friendUidToName,
                              OnRsvpClickListener listener) {
        this.mContext         = context;
        this.mEvents          = events;
        this.mFriendUidToName = friendUidToName;
        this.mListener        = listener;
    }

    public void updateData(List<Event> events, Map<String, String> friendUidToName) {
        mEvents          = events;
        mFriendUidToName = friendUidToName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_friends_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = mEvents.get(position);

        // Image
        Glide.with(holder.imageViewEvent.getContext())
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(holder.imageViewEvent);

        // Title, location, datetime
        holder.textViewTitle.setText(event.getTitle());
        holder.textViewLocation.setText(event.getLocation());
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE | h:mma", Locale.getDefault());
        holder.textViewDateTime.setText(sdf.format(new Date(event.getDateTime())));

        // Friends attending text
        holder.textViewFriendsAttending.setText(buildFriendsText(event));

        // RSVP button
        String  uid    = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : "";
        boolean rsvped = event.isRsvped(uid);
        holder.buttonRsvp.setBackground(ContextCompat.getDrawable(mContext,
                rsvped ? R.drawable.bg_btn_going : R.drawable.bg_btn_rsvp));
        holder.buttonRsvp.setText(rsvped ? "✓ Going" : "RSVP");
        holder.buttonRsvp.setTextColor(rsvped
                ? ContextCompat.getColor(mContext, R.color.color_button_going_stroke)
                : Color.WHITE);
        holder.buttonRsvp.setOnClickListener(v -> {
            int p = holder.getAdapterPosition();
            if (p != RecyclerView.NO_ID) mListener.onRsvpClick(event, p);
        });
    }

    /** Builds "Ahmed, Eman and Ali have RSVP'd" (max 3 names, then "and more"). */
    private String buildFriendsText(Event event) {
        if (event.getAttendeeIds() == null || mFriendUidToName == null) return "";

        List<String> names = new ArrayList<>();
        for (String uid : event.getAttendeeIds()) {
            String name = mFriendUidToName.get(uid);
            if (name != null) names.add(name.split(" ")[0]);
        }

        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0) + " has RSVP'd";
        if (names.size() == 2) return names.get(0) + " and " + names.get(1) + " have RSVP'd";
        String base = names.get(0) + ", " + names.get(1) + " and " + names.get(2);
        if (names.size() == 3) return base + " have RSVP'd";
        return base + " and more have RSVP'd";
    }

    @Override
    public int getItemCount() {
        return mEvents == null ? 0 : mEvents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageViewEvent;
        final TextView  textViewTitle;
        final TextView  textViewDateTime;
        final TextView  textViewLocation;
        final TextView  textViewFriendsAttending;
        final Button    buttonRsvp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewEvent           = itemView.findViewById(R.id.imageViewEvent);
            textViewTitle            = itemView.findViewById(R.id.textViewTitle);
            textViewDateTime         = itemView.findViewById(R.id.textViewDateTime);
            textViewLocation         = itemView.findViewById(R.id.textViewLocation);
            textViewFriendsAttending = itemView.findViewById(R.id.textViewFriendsAttending);
            buttonRsvp               = itemView.findViewById(R.id.buttonRsvp);
        }
    }
}
