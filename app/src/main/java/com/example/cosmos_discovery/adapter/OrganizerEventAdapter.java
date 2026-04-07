package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for organizer "Posted Events" cards.
 *
 * Uses {@code item_event_card_organizer.xml} (a variant of the small event card)
 * and shows a status badge instead of an RSVP button.
 */
public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final Context             mContext;
    private final OnEventClickListener mListener;
    private List<Event>               mEvents;

    public OrganizerEventAdapter(Context context, List<Event> events, OnEventClickListener listener) {
        mContext  = context;
        mEvents   = events;
        mListener = listener;
    }

    public void updateData(List<Event> events) {
        mEvents = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card_organizer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = mEvents.get(position);

        Glide.with(holder.imageViewEvent.getContext())
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(holder.imageViewEvent);

        holder.textViewTitle.setText(event.getTitle());
        holder.textViewLocation.setText(event.getLocation());

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d | h:mma", Locale.getDefault());
        holder.textViewDateTime.setText(sdf.format(new Date(event.getDateTime())));

        holder.textViewRsvpCount.setText(event.getRsvpCount() + " RSVPs");

        holder.chipGroupTags.removeAllViews();
        if (event.getTags() != null) {
            for (String tag : event.getTags()) {
                TextView chip = new TextView(mContext);
                chip.setText(tag);
                chip.setTextSize(9f);
                chip.setTextColor(ContextCompat.getColor(mContext, R.color.color_chip_text));
                chip.setBackground(ContextCompat.getDrawable(mContext, R.drawable.bg_chip_outline));
                chip.setPadding(
                        (int) dpToPx(7.85f),
                        (int) dpToPx(2.94f),
                        (int) dpToPx(7.85f),
                        (int) dpToPx(2.94f));
                chip.setIncludeFontPadding(false);
                ChipGroup.LayoutParams lp = new ChipGroup.LayoutParams(
                        ChipGroup.LayoutParams.WRAP_CONTENT,
                        ChipGroup.LayoutParams.WRAP_CONTENT);
                chip.setLayoutParams(lp);
                holder.chipGroupTags.addView(chip);
            }
        }

        holder.itemView.setOnClickListener(v -> mListener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return mEvents == null ? 0 : mEvents.size();
    }

    private float dpToPx(float dp) {
        return dp * mContext.getResources().getDisplayMetrics().density;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView        imageViewEvent;
        final TextView         textViewTitle;
        final TextView         textViewDateTime;
        final TextView         textViewLocation;
        final ChipGroup        chipGroupTags;
        final TextView         textViewRsvpCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewEvent    = itemView.findViewById(R.id.imageViewEvent);
            textViewTitle     = itemView.findViewById(R.id.textViewTitle);
            textViewDateTime  = itemView.findViewById(R.id.textViewDateTime);
            textViewLocation  = itemView.findViewById(R.id.textViewLocation);
            chipGroupTags     = itemView.findViewById(R.id.chipGroupTags);
            textViewRsvpCount = itemView.findViewById(R.id.textViewRsvpCount);
        }
    }
}

