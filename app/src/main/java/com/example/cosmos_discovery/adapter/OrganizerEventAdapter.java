package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.database.NotificationService;
import com.example.cosmos_discovery.model.Notification;
import com.example.cosmos_discovery.ui.organizer.AnnouncementsActivity;
import com.example.cosmos_discovery.ui.organizer.AttendeeListActivity;
import com.example.cosmos_discovery.ui.organizer.EditEventActivity;
import com.example.cosmos_discovery.ui.organizer.EventStatsActivity;
import com.example.cosmos_discovery.util.OrganizerEventMenuHelper;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private final Context              mContext;
    private final OnEventClickListener mListener;
    private List<Event>                mEvents;
    private final @DrawableRes int     mAccentDrawable;
    private final boolean              mHorizontal;

    public OrganizerEventAdapter(Context context, List<Event> events, OnEventClickListener listener) {
        this(context, events, 0, false, listener);
    }

    public OrganizerEventAdapter(Context context, List<Event> events,
                                 @DrawableRes int accentDrawable,
                                 OnEventClickListener listener) {
        this(context, events, accentDrawable, false, listener);
    }

    public OrganizerEventAdapter(Context context, List<Event> events,
                                 @DrawableRes int accentDrawable,
                                 boolean horizontal,
                                 OnEventClickListener listener) {
        mContext         = context;
        mEvents          = events;
        mAccentDrawable  = accentDrawable;
        mHorizontal      = horizontal;
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
                .inflate(R.layout.item_event_card_organizer, parent, false);
        if (mHorizontal) {
            int cardWidth = (int) (parent.getContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.82f);
            view.getLayoutParams().width = cardWidth;
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = mEvents.get(position);

        // Accent border
        if (mAccentDrawable != 0) {
            holder.accentBorder.setBackgroundResource(mAccentDrawable);
            holder.accentBorder.setVisibility(View.VISIBLE);
        } else {
            holder.accentBorder.setVisibility(View.GONE);
        }

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

        if (holder.btnOverflow != null) {
            holder.btnOverflow.setOnClickListener(v -> showEventMenu(v, event));
        }
    }

    private void showEventMenu(View anchor, Event event) {
        PopupMenu menu = new PopupMenu(mContext, anchor);
        menu.getMenuInflater().inflate(R.menu.menu_event_details, menu.getMenu());
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(menu.getMenu(), event);

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                openEdit(event);
            } else if (id == R.id.action_announcement) {
                openAnnouncements(event);
            } else if (id == R.id.action_stats) {
                openStats(event);
            } else if (id == R.id.action_attendee_list) {
                openAttendees(event);
            } else if (id == R.id.action_cancel) {
                confirmCancel(event);
            } else if (id == R.id.action_delete) {
                confirmDelete(event);
            } else {
                return false;
            }
            return true;
        });
        menu.show();
    }

    private void openStats(Event event) {
        Intent i = new Intent(mContext, EventStatsActivity.class);
        i.putExtra(EventStatsActivity.EXTRA_EVENT_ID, event.getId());
        mContext.startActivity(i);
    }

    private void openAttendees(Event event) {
        Intent i = new Intent(mContext, AttendeeListActivity.class);
        i.putExtra(AttendeeListActivity.EXTRA_EVENT_ID, event.getId());
        mContext.startActivity(i);
    }

    private void confirmDelete(Event event) {
        new AlertDialog.Builder(mContext)
                .setTitle("Are you sure you want to delete this event?")
                .setMessage("This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> deleteEvent(event))
                .show();
    }

    private void deleteEvent(Event event) {
        new EventService().removeEvent(
                event.getId(),
                () -> {
                    notifyAttendeesOfCancellation(event);
                    Toast.makeText(mContext, "Event deleted.", Toast.LENGTH_SHORT).show();
                },
                err -> Toast.makeText(mContext, err, Toast.LENGTH_LONG).show()
        );
    }

    private void notifyAttendeesOfCancellation(Event event) {
        if (event.getAttendeeIds() == null || event.getAttendeeIds().isEmpty()) return;

        List<String> recipients = new ArrayList<>(event.getAttendeeIds());
        String organizerUid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid() : null;
        if (organizerUid != null) recipients.remove(organizerUid);
        if (recipients.isEmpty()) return;

        Notification notif = new Notification(
                Notification.TYPE_EVENT_CANCELLED,
                "Event Cancelled",
                "Sorry, " + event.getTitle() + " has been cancelled.",
                System.currentTimeMillis()
        );
        notif.setEventTitle(event.getTitle());
        notif.setAudience(Notification.AUDIENCE_PERSONAL);

        new NotificationService()
                .writeNotificationToUsers(recipients, notif, () -> {}, err -> {});
    }

    private void openAnnouncements(Event event) {
        Intent i = new Intent(mContext, AnnouncementsActivity.class);
        i.putExtra(AnnouncementsActivity.EXTRA_EVENT_ID,    event.getId());
        i.putExtra(AnnouncementsActivity.EXTRA_EVENT_TITLE, event.getTitle());
        mContext.startActivity(i);
    }

    private void openEdit(Event event) {
        Intent i = new Intent(mContext, EditEventActivity.class);
        i.putExtra(EditEventActivity.EXTRA_EVENT_ID, event.getId());
        mContext.startActivity(i);
    }

    private void confirmCancel(Event event) {
        new AlertDialog.Builder(mContext)
                .setTitle("Cancel event?")
                .setMessage("All RSVP'd attendees will be notified that \""
                        + event.getTitle() + "\" is cancelled. This cannot be undone.")
                .setNegativeButton("Keep event", null)
                .setPositiveButton("Cancel event", (d, w) -> {
                    String uid = RoleUtil.getCurrentUser() != null
                            ? RoleUtil.getCurrentUser().getUid() : null;
                    new EventService().cancelEvent(event.getId(), uid,
                            () -> Toast.makeText(mContext, "Event cancelled.", Toast.LENGTH_SHORT).show(),
                            err -> Toast.makeText(mContext, err, Toast.LENGTH_LONG).show());
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return mEvents == null ? 0 : mEvents.size();
    }

    private float dpToPx(float dp) {
        return dp * mContext.getResources().getDisplayMetrics().density;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View             accentBorder;
        final ImageView        imageViewEvent;
        final TextView         textViewTitle;
        final TextView         textViewDateTime;
        final TextView         textViewLocation;
        final ChipGroup        chipGroupTags;
        final TextView         textViewRsvpCount;
        final ImageView        btnOverflow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            accentBorder      = itemView.findViewById(R.id.accentBorder);
            imageViewEvent    = itemView.findViewById(R.id.imageViewEvent);
            textViewTitle     = itemView.findViewById(R.id.textViewTitle);
            textViewDateTime  = itemView.findViewById(R.id.textViewDateTime);
            textViewLocation  = itemView.findViewById(R.id.textViewLocation);
            chipGroupTags     = itemView.findViewById(R.id.chipGroupTags);
            textViewRsvpCount = itemView.findViewById(R.id.textViewRsvpCount);
            btnOverflow       = itemView.findViewById(R.id.btnOverflow);
        }
    }
}

