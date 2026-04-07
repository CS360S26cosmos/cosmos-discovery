package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
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
 * RecyclerView adapter for organizer big event cards in horizontal carousels.
 * Used for Pending and Approved sections on the "My Posted Events" page.
 *
 * Similar to {@link EventBigAdapter} but without an RSVP button and with
 * a colored left-border accent to indicate event status.
 */
public class OrganizerBigCardAdapter extends RecyclerView.Adapter<OrganizerBigCardAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final Context              mContext;
    private List<Event>                mEvents;
    private final @DrawableRes int     mAccentDrawable;
    private final OnEventClickListener mListener;

    public OrganizerBigCardAdapter(Context context, List<Event> events,
                                   @DrawableRes int accentDrawable,
                                   OnEventClickListener listener) {
        mContext         = context;
        mEvents          = events;
        mAccentDrawable  = accentDrawable;
        mListener        = listener;
    }

    public void updateData(List<Event> events) {
        mEvents = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card_organizer_big, parent, false);
        int cardWidth = (int) (parent.getContext().getResources()
                .getDisplayMetrics().widthPixels * 0.82f);
        view.getLayoutParams().width = cardWidth;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = mEvents.get(position);

        // Accent border
        holder.accentBorder.setBackgroundResource(mAccentDrawable);

        // Image
        Glide.with(holder.imageViewEvent.getContext())
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(holder.imageViewEvent);

        // Title & location
        holder.textViewTitle.setText(event.getTitle());
        holder.textViewLocation.setText(event.getLocation());

        // DateTime
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy | h:mma", Locale.getDefault());
        holder.textViewDateTime.setText(sdf.format(new Date(event.getDateTime())));

        // RSVP count
        holder.textViewRsvpCount.setText(event.getRsvpCount() + " RSVPs");

        // Chips
        holder.chipGroupTags.removeAllViews();
        if (event.getTags() != null) {
            for (String tag : event.getTags()) {
                TextView chip = new TextView(mContext);
                chip.setText(tag);
                chip.setTextSize(10f);
                chip.setTextColor(ContextCompat.getColor(mContext, R.color.color_chip_text));
                chip.setBackground(ContextCompat.getDrawable(mContext, R.drawable.bg_chip_outline));
                chip.setPadding(
                        (int) dpToPx(9f),
                        (int) dpToPx(4f),
                        (int) dpToPx(9f),
                        (int) dpToPx(4f));
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
        final View      accentBorder;
        final ImageView imageViewEvent;
        final TextView  textViewTitle;
        final TextView  textViewDateTime;
        final TextView  textViewLocation;
        final ChipGroup chipGroupTags;
        final TextView  textViewRsvpCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            accentBorder      = itemView.findViewById(R.id.accentBorder);
            imageViewEvent    = itemView.findViewById(R.id.imageViewEvent);
            textViewTitle     = itemView.findViewById(R.id.textViewTitle);
            textViewDateTime  = itemView.findViewById(R.id.textViewDateTime);
            textViewLocation  = itemView.findViewById(R.id.textViewLocation);
            chipGroupTags     = itemView.findViewById(R.id.chipGroupTags);
            textViewRsvpCount = itemView.findViewById(R.id.textViewRsvpCount);
        }
    }
}
