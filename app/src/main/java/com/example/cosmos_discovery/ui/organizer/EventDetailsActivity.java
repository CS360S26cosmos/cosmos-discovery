package com.example.cosmos_discovery.ui.organizer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.CalendarUtil;
import com.example.cosmos_discovery.util.OrganizerEventMenuHelper;
import com.example.cosmos_discovery.util.RoleUtil;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private final EventService mEventService = new EventService();
    private final RsvpHandler  mRsvpHandler  = new RsvpHandler();

    private String mEventId;
    private Event  mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_details_page);

        mEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageButton btnOverflowMenu = findViewById(R.id.btnOverflowMenu);
        btnOverflowMenu.setVisibility(View.GONE); // hidden until event loads; shown only for the event's owner
        btnOverflowMenu.setOnClickListener(v -> showOverflowMenu(btnOverflowMenu));

        wireBottomNav();

        fetchAndBind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from EditEventActivity.
        fetchAndBind();
    }

    private void fetchAndBind() {
        mEventService.fetchEventById(
                mEventId,
                event -> {
                    mEvent = event;
                    bindEvent(event);
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private void bindEvent(Event event) {
        ImageView image = findViewById(R.id.imageViewEvent);
        TextView title  = findViewById(R.id.textViewEventTitle);

        TextView registerBy = findViewById(R.id.tvRegisterByValue);
        TextView venue      = findViewById(R.id.tvVenueValue);
        TextView dateTime   = findViewById(R.id.tvDateTimeValue);
        TextView organizer  = findViewById(R.id.tvOrganizerValue);
        TextView desc       = findViewById(R.id.tvDescriptionValue);

        Glide.with(this)
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(image);

        title.setText(event.getTitle() != null ? event.getTitle() : "Event");

        String registerByText = event.getRegisterBy();
        if (registerByText == null || registerByText.trim().isEmpty()) {
            registerByText = "-";
        }
        registerBy.setText(registerByText);

        venue.setText(event.getLocation() != null ? event.getLocation() : "-");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy | h:mma", Locale.getDefault());
        dateTime.setText(sdf.format(new Date(event.getDateTime())));

        String orgName = "Organizer";
        if (RoleUtil.getCurrentUser() != null && event.getOrganizerId() != null
                && event.getOrganizerId().equals(RoleUtil.getCurrentUser().getUid())) {
            orgName = "You";
        } else if (event.getOrganizerName() != null && !event.getOrganizerName().trim().isEmpty()) {
            orgName = event.getOrganizerName();
        }
        organizer.setText(orgName);

        String description = event.getDescription();
        desc.setText(description != null && !description.trim().isEmpty()
                ? description
                : "No description provided.");

        boolean isEventOwner = RoleUtil.getCurrentUser() != null
                && event.getOrganizerId() != null
                && event.getOrganizerId().equals(RoleUtil.getCurrentUser().getUid());
        findViewById(R.id.btnOverflowMenu).setVisibility(isEventOwner ? View.VISIBLE : View.GONE);

        View capacityRow = findViewById(R.id.capacityRow);
        TextView capacityInfo = findViewById(R.id.tvCapacityInfo);
        if (event.hasCapacity()) {
            capacityRow.setVisibility(View.VISIBLE);
            capacityInfo.setText(event.getSpotsText());
        } else {
            capacityRow.setVisibility(View.GONE);
        }

        MaterialCardView addToCalendarCard = findViewById(R.id.addToCalendarCard);
        if (addToCalendarCard != null) {
            addToCalendarCard.setOnClickListener(v -> {
                Intent calIntent = CalendarUtil.buildCalendarIntent(mEvent);
                try {
                    startActivity(calIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "No calendar app found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        MaterialCardView shareCard = findViewById(R.id.shareButtonCard);
        if (shareCard != null) {
            shareCard.setOnClickListener(v -> shareEvent(event));
        }

        // Category
        LinearLayout categoryRow = findViewById(R.id.categoryRow);
        TextView tvCategory = findViewById(R.id.tvCategoryValue);
        String category = event.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            tvCategory.setText(category);
            categoryRow.setVisibility(View.VISIBLE);
        } else {
            categoryRow.setVisibility(View.GONE);
        }

        // Rejection reason (organizer/owner only, when status = rejected)
        LinearLayout rejectionRow = findViewById(R.id.rejectionReasonRow);
        if (isEventOwner && Event.STATUS_REJECTED.equals(event.getStatus())) {
            String rejReason = event.getRejectionReason();
            TextView tvRejReason = findViewById(R.id.tvRejectionReasonDetail);
            tvRejReason.setText(rejReason != null && !rejReason.trim().isEmpty()
                    ? rejReason : "No reason provided.");
            rejectionRow.setVisibility(View.VISIBLE);
        } else {
            rejectionRow.setVisibility(View.GONE);
        }

        loadOrganizerRating(event.getOrganizerId());

        MaterialCardView rsvpCard = findViewById(R.id.rsvpButton);
        TextView tvRsvp = findViewById(R.id.tvRsvp);
        if (rsvpCard != null && tvRsvp != null) {
            boolean isOwner = RoleUtil.getCurrentUser() != null
                    && event.getOrganizerId() != null
                    && event.getOrganizerId().equals(RoleUtil.getCurrentUser().getUid());
            if (isOwner) {
                rsvpCard.setVisibility(View.GONE);
            } else {
                rsvpCard.setVisibility(View.VISIBLE);
                bindRsvpButton(event, rsvpCard, tvRsvp);
            }
        }
    }

    private void loadOrganizerRating(String organizerId) {
        LinearLayout ratingRow = findViewById(R.id.ratingRow);
        TextView tvRating = findViewById(R.id.tvEventRatingValue);
        if (organizerId == null || organizerId.isEmpty()) {
            if (ratingRow != null) ratingRow.setVisibility(View.GONE);
            return;
        }
        mEventService.fetchOrganizerAverageRating(organizerId,
                avg -> {
                    if (avg != null && ratingRow != null && tvRating != null) {
                        tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f / 5", avg));
                        ratingRow.setVisibility(View.VISIBLE);
                    } else if (ratingRow != null) {
                        ratingRow.setVisibility(View.GONE);
                    }
                },
                err -> { if (ratingRow != null) ratingRow.setVisibility(View.GONE); }
        );
    }

    private void shareEvent(Event event) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d 'at' h:mma", java.util.Locale.getDefault());
        String body = event.getTitle()
                + "\n" + sdf.format(new Date(event.getDateTime()))
                + (event.getLocation() != null ? "\n" + event.getLocation() : "");
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, event.getTitle());
        share.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(share, "Share event"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindRsvpButton(Event event, MaterialCardView card, TextView label) {
        String uid = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : "";
        boolean rsvped = event.isRsvped(uid);

        if (!rsvped && event.isRegistrationClosed()) {
            label.setText("Closed");
            card.setEnabled(false);
            card.setAlpha(0.5f);
            card.setOnClickListener(null);
        } else if (!rsvped && event.isFull()) {
            label.setText("Full");
            card.setEnabled(false);
            card.setAlpha(0.5f);
            card.setOnClickListener(null);
        } else {
            label.setText(rsvped ? "✓ Going" : "RSVP");
            card.setEnabled(true);
            card.setAlpha(1.0f);
            card.setOnClickListener(v -> mRsvpHandler.toggle(
                    event,
                    () -> bindRsvpButton(event, card, label),
                    err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            ));
        }
    }

    private void showOverflowMenu(ImageButton anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_event_details, popup.getMenu());
        OrganizerEventMenuHelper.applyOrganizerMenuVisibility(popup.getMenu(), mEvent);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                Intent intent = new Intent(this, EditEventActivity.class);
                intent.putExtra(EditEventActivity.EXTRA_EVENT_ID, mEventId);
                startActivity(intent);
            } else if (id == R.id.action_announcement) {
                Intent intent = new Intent(this, AnnouncementsActivity.class);
                intent.putExtra(AnnouncementsActivity.EXTRA_EVENT_ID, mEventId);
                intent.putExtra(AnnouncementsActivity.EXTRA_EVENT_TITLE,
                        mEvent != null ? mEvent.getTitle() : "");
                startActivity(intent);
            } else if (id == R.id.action_attendee_list) {
                Intent intent = new Intent(this, AttendeeListActivity.class);
                intent.putExtra(AttendeeListActivity.EXTRA_EVENT_ID, mEventId);
                startActivity(intent);
            } else if (id == R.id.action_stats) {
                Intent intent = new Intent(this, EventStatsActivity.class);
                intent.putExtra(EventStatsActivity.EXTRA_EVENT_ID, mEventId);
                startActivity(intent);
            } else if (id == R.id.action_cancel) {
                confirmCancel();
            } else if (id == R.id.action_delete) {
                confirmDelete();
            }
            return true;
        });
        popup.show();
    }

    private void confirmCancel() {
        if (mEvent == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Cancel event?")
                .setMessage("All RSVP'd attendees will be notified that \""
                        + mEvent.getTitle() + "\" is cancelled. This cannot be undone.")
                .setNegativeButton("Keep event", null)
                .setPositiveButton("Cancel event", (d, w) -> {
                    String uid = RoleUtil.getCurrentUser() != null
                            ? RoleUtil.getCurrentUser().getUid() : null;
                    mEventService.cancelEvent(mEventId, uid,
                            () -> {
                                Toast.makeText(this, "Event cancelled.", Toast.LENGTH_SHORT).show();
                                fetchAndBind();
                            },
                            err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Are you sure you want to delete this event?")
                .setMessage("This action cannot be undone. All attendees will be notified that the event has been cancelled.")
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Delete", (d, which) -> deleteEvent())
                .show();
    }

    private void deleteEvent() {
        mEventService.removeEvent(
                mEventId,
                () -> {
                    notifyAttendeesOfCancellation();
                    Toast.makeText(this, "Event deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private void notifyAttendeesOfCancellation() {
        if (mEvent == null || mEvent.getAttendeeIds() == null || mEvent.getAttendeeIds().isEmpty()) return;

        List<String> recipients = new ArrayList<>(mEvent.getAttendeeIds());
        String organizerUid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid() : null;
        if (organizerUid != null) recipients.remove(organizerUid);
        if (recipients.isEmpty()) return;

        com.example.cosmos_discovery.model.Notification notif =
                new com.example.cosmos_discovery.model.Notification(
                        com.example.cosmos_discovery.model.Notification.TYPE_EVENT_CANCELLED,
                        "Event Cancelled",
                        "Sorry, " + mEvent.getTitle() + " has been cancelled.",
                        System.currentTimeMillis()
                );
        notif.setEventTitle(mEvent.getTitle());
        notif.setAudience(com.example.cosmos_discovery.model.Notification.AUDIENCE_PERSONAL);

        new com.example.cosmos_discovery.database.NotificationService()
                .writeNotificationToUsers(recipients, notif, () -> {}, err -> {});
    }

    private void wireBottomNav() {
        if (findViewById(R.id.navHome) == null) return;
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentActivity.class);
            intent.putExtra(StudentActivity.EXTRA_START_TAB, StudentActivity.TAB_DISCOVER);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentActivity.class);
            intent.putExtra(StudentActivity.EXTRA_START_TAB, StudentActivity.TAB_MY_EVENTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        findViewById(R.id.navFriends).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentActivity.class);
            intent.putExtra(StudentActivity.EXTRA_START_TAB, StudentActivity.TAB_FRIENDS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}
