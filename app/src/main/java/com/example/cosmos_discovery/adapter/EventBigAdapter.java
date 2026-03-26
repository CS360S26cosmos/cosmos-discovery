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
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the full-width "Discover" big event cards
 * ({@code item_event_card_big.xml}).
 *
 * The fragment that hosts the RecyclerView should implement {@link OnRsvpClickListener}
 * and pass itself as the listener. The fragment is responsible for calling
 * {@link EventService#rsvpToEvent} or {@link EventService#cancelRsvp} in response.
 */
public class EventBigAdapter extends RecyclerView.Adapter<EventBigAdapter.ViewHolder> {

    public interface OnRsvpClickListener {
        void onRsvpClick(Event event, int position);
    }

    private List<Event>               mEvents;
    private final OnRsvpClickListener mListener;
    private final Context             mContext;

    public EventBigAdapter(Context context, List<Event> events, OnRsvpClickListener listener) {
        this.mContext  = context;
        this.mEvents   = events;
        this.mListener = listener;
    }

    /** Replaces the data set and refreshes the list. Call after a Firestore fetch completes. */
    public void updateData(List<Event> events) {
        mEvents = events;
        notifyDataSetChanged();
    }

    /**
     * Inflates {@code item_event_card_big.xml} and fixes each card's width at 82% of the
     * screen width so the right edge of the next card peeks into view, signalling
     * that the carousel is horizontally scrollable.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card_big, parent, false);
        int cardWidth = (int) (parent.getContext().getResources().getDisplayMetrics().widthPixels * 0.82f);
        view.getLayoutParams().width = cardWidth;
        return new ViewHolder(view);
    }

    /**
     * Binds an {@link Event} to the ViewHolder: loads the image via Glide, sets title/location/
     * datetime/RSVP count, inflates tag chips, and toggles the RSVP button appearance based
     * on whether the current user has already RSVPed.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = mEvents.get(position);

        // Image
        Glide.with(holder.imageViewEvent.getContext())
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(holder.imageViewEvent);

        // Title & location
        holder.textViewTitle.setText(event.getTitle());
        holder.textViewLocation.setText(event.getLocation());

        // DateTime — "18th March 2026 | 7:00pm"
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy | h:mma", Locale.getDefault());
        holder.textViewDateTime.setText(sdf.format(new Date(event.getDateTime())));

        // RSVP count
        holder.textViewRsvpCount.setText(event.getRsvpCount() + " RSVPs");

        // Chips — plain TextViews with a stroke-only drawable background
        // (Material Chip's M3 surface tint overlay cannot be fully disabled)
        holder.chipGroupTags.removeAllViews();
        if (event.getTags() != null) {
            for (String tag : event.getTags()) {
                TextView chip = new TextView(mContext);
                chip.setText(tag);
                chip.setTextSize(10f);
                chip.setTextColor(ContextCompat.getColor(mContext, R.color.color_chip_text));
                chip.setBackground(ContextCompat.getDrawable(mContext, R.drawable.bg_chip_outline));
                chip.setPadding(
                        (int) dpToPx(9f),   // start
                        (int) dpToPx(4f),   // top
                        (int) dpToPx(9f),   // end
                        (int) dpToPx(4f));  // bottom
                chip.setIncludeFontPadding(false);
                ChipGroup.LayoutParams lp = new ChipGroup.LayoutParams(
                        ChipGroup.LayoutParams.WRAP_CONTENT,
                        ChipGroup.LayoutParams.WRAP_CONTENT);
                chip.setLayoutParams(lp);
                holder.chipGroupTags.addView(chip);
            }
        }

        // RSVP button — toggle appearance based on current user's RSVP state
        String uid    = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : "";
        boolean rsvped = event.isRsvped(uid);

        holder.buttonRsvp.setBackground(ContextCompat.getDrawable(mContext,
                rsvped ? R.drawable.bg_btn_going : R.drawable.bg_btn_rsvp));
        holder.buttonRsvp.setText(rsvped ? "✓ Going" : "RSVP");
        holder.buttonRsvp.setTextColor(rsvped
                ? ContextCompat.getColor(mContext, R.color.color_button_going_stroke)
                : Color.WHITE);

        holder.buttonRsvp.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_ID) {
                mListener.onRsvpClick(event, adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEvents == null ? 0 : mEvents.size();
    }

    /** Converts dp to pixels using the current display density. */
    private float dpToPx(float dp) {
        return dp * mContext.getResources().getDisplayMetrics().density;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageViewEvent;
        final TextView  textViewTitle;
        final TextView  textViewDateTime;
        final TextView  textViewLocation;
        final ChipGroup chipGroupTags;
        final TextView  textViewRsvpCount;
        final Button    buttonRsvp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewEvent    = itemView.findViewById(R.id.imageViewEvent);
            textViewTitle     = itemView.findViewById(R.id.textViewTitle);
            textViewDateTime  = itemView.findViewById(R.id.textViewDateTime);
            textViewLocation  = itemView.findViewById(R.id.textViewLocation);
            chipGroupTags     = itemView.findViewById(R.id.chipGroupTags);
            textViewRsvpCount = itemView.findViewById(R.id.textViewRsvpCount);
            buttonRsvp        = itemView.findViewById(R.id.buttonRsvp);
        }
    }
}
