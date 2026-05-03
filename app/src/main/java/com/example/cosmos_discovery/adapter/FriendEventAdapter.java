package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
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

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private List<Event>                mEvents;
    private Map<String, String>        mFriendUidToName;
    private final OnRsvpClickListener  mListener;
    private final OnEventClickListener mCardListener;
    private final Context              mContext;

    public FriendEventAdapter(Context context, List<Event> events,
                              Map<String, String> friendUidToName,
                              OnRsvpClickListener listener) {
        this(context, events, friendUidToName, listener, null);
    }

    public FriendEventAdapter(Context context, List<Event> events,
                              Map<String, String> friendUidToName,
                              OnRsvpClickListener rsvpListener,
                              OnEventClickListener cardListener) {
        this.mContext         = context;
        this.mEvents          = events;
        this.mFriendUidToName = friendUidToName;
        this.mListener        = rsvpListener;
        this.mCardListener    = cardListener;
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

        // Friends attending text — progressively shorten if ellipsized
        CharSequence[] candidates = buildFriendsTexts(event);
        setFriendsTextWithFallback(holder.textViewFriendsAttending, candidates, 0);

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

        if (mCardListener != null) {
            holder.itemView.setOnClickListener(v -> mCardListener.onEventClick(event));
        }
    }

    /**
     * Tries candidates[index] first; if it is ellipsized after layout, moves to the next
     * candidate. The last candidate is always short enough to fit.
     */
    private void setFriendsTextWithFallback(TextView tv, CharSequence[] candidates, int index) {
        tv.setText(candidates[index], TextView.BufferType.SPANNABLE);
        if (index < candidates.length - 1) {
            int next = index + 1;
            tv.post(() -> {
                android.text.Layout layout = tv.getLayout();
                if (layout != null && layout.getEllipsisCount(0) > 0) {
                    setFriendsTextWithFallback(tv, candidates, next);
                }
            });
        }
    }

    /** Appends boldText in bold followed by normalText in normal weight. */
    private static SpannableStringBuilder boldThen(String boldText, String normalText) {
        SpannableStringBuilder sb = new SpannableStringBuilder(boldText);
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), 0);
        sb.append(normalText);
        return sb;
    }

    /**
     * Builds up to 3 candidate CharSequences in decreasing length (names are bold):
     *   [0] "Sameen, Hamania and Ahmed have RSVP'd"   (3 names)
     *   [1] "Sameen, Hamania and more have RSVP'd"    (2 names — fallback)
     *   [2] "Sameen and N more friends have RSVP'd"   (1 name  — last resort)
     * For 1–2 friends only one candidate is returned.
     */
    private CharSequence[] buildFriendsTexts(Event event) {
        if (event.getAttendeeIds() == null || mFriendUidToName == null)
            return new CharSequence[]{""};

        List<String> names = new ArrayList<>();
        for (String uid : event.getAttendeeIds()) {
            String raw = mFriendUidToName.get(uid);
            if (raw != null) {
                String first = raw.split(" ")[0];
                names.add(Character.toUpperCase(first.charAt(0)) + first.substring(1));
            }
        }

        if (names.isEmpty()) return new CharSequence[]{""};
        if (names.size() == 1)
            return new CharSequence[]{boldThen(names.get(0), " has RSVP'd")};
        if (names.size() == 2) {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(boldThen(names.get(0), " and "));
            sb.append(boldThen(names.get(1), " have RSVP'd"));
            return new CharSequence[]{sb};
        }

        int others = names.size() - 1;

        // Candidate 0: three names
        SpannableStringBuilder c0 = new SpannableStringBuilder();
        c0.append(boldThen(names.get(0), ", "));
        c0.append(boldThen(names.get(1), " and "));
        c0.append(boldThen(names.get(2), " have RSVP'd"));

        // Candidate 1: two names + "and more"
        SpannableStringBuilder c1 = new SpannableStringBuilder();
        c1.append(boldThen(names.get(0), ", "));
        c1.append(boldThen(names.get(1), " and more have RSVP'd"));

        // Candidate 2: one name + count (always fits)
        CharSequence c2 = boldThen(names.get(0), " and " + others + " more friends have RSVP'd");

        return new CharSequence[]{c0, c1, c2};
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
