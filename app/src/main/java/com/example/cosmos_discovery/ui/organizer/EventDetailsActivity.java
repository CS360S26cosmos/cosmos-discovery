package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.example.cosmos_discovery.util.RoleUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private final EventService mEventService = new EventService();

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
        btnOverflowMenu.setOnClickListener(v -> showOverflowMenu(btnOverflowMenu));

        // Organizers shouldn't RSVP to their own events.
        if (findViewById(R.id.rsvpButton) != null) findViewById(R.id.rsvpButton).setVisibility(android.view.View.GONE);

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

        View capacityRow = findViewById(R.id.capacityRow);
        TextView capacityInfo = findViewById(R.id.tvCapacityInfo);
        if (event.hasCapacity()) {
            capacityRow.setVisibility(View.VISIBLE);
            capacityInfo.setText(event.getSpotsText());
        } else {
            capacityRow.setVisibility(View.GONE);
        }
    }

    private void showOverflowMenu(ImageButton anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_event_details, popup.getMenu());

        // Attendee List is organizer-only.
        boolean isOrganizer = mEvent != null
                && RoleUtil.getCurrentUser() != null
                && mEvent.getOrganizerId() != null
                && mEvent.getOrganizerId().equals(RoleUtil.getCurrentUser().getUid());
        popup.getMenu().findItem(R.id.action_attendee_list).setVisible(isOrganizer);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                Intent intent = new Intent(this, EditEventActivity.class);
                intent.putExtra(EditEventActivity.EXTRA_EVENT_ID, mEventId);
                startActivity(intent);
            } else if (id == R.id.action_attendee_list) {
                Intent intent = new Intent(this, AttendeeListActivity.class);
                intent.putExtra(AttendeeListActivity.EXTRA_EVENT_ID, mEventId);
                startActivity(intent);
            } else if (id == R.id.action_stats) {
                Intent intent = new Intent(this, EventStatsActivity.class);
                intent.putExtra(EventStatsActivity.EXTRA_EVENT_ID, mEventId);
                startActivity(intent);
            } else if (id == R.id.action_delete) {
                confirmDelete();
            }
            return true;
        });
        popup.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete event")
                .setMessage("Are you sure you want to delete this event?")
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Delete", (d, which) -> deleteEvent())
                .show();
    }

    private void deleteEvent() {
        mEventService.removeEvent(
                mEventId,
                () -> {
                    Toast.makeText(this, "Event deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
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
