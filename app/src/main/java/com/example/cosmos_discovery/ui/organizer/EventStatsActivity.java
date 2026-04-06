package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.AttendeeAdapter;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.EventStatusUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class EventStatsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private final EventService  mEventService  = new EventService();
    private final FriendService mFriendService = new FriendService();

    private String mEventId;

    private TextView         mTvEventTitle;
    private MaterialCardView mStatusBadgeCard;
    private TextView         mTvStatusBadge;
    private TextView         mTvTotalRsvps;
    private TextView         mTvEmptyAttendees;
    private AttendeeAdapter  mAttendeeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_stats);

        mEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        mTvEventTitle     = findViewById(R.id.tvEventTitle);
        mStatusBadgeCard  = findViewById(R.id.statusBadgeCard);
        mTvStatusBadge    = findViewById(R.id.tvStatusBadge);
        mTvTotalRsvps     = findViewById(R.id.tvTotalRsvps);
        mTvEmptyAttendees = findViewById(R.id.tvEmptyAttendees);

        RecyclerView rv = findViewById(R.id.rvAttendees);
        mAttendeeAdapter = new AttendeeAdapter(this, new ArrayList<>());
        rv.setAdapter(mAttendeeAdapter);

        wireBottomNav();
        fetchAndBind();
    }

    private void fetchAndBind() {
        mEventService.fetchEventById(
                mEventId,
                event -> {
                    bindEvent(event);
                    loadAttendees(event);
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private void bindEvent(Event event) {
        mTvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "Event");

        String status = event.getStatus();
        mTvStatusBadge.setText(EventStatusUtil.toDisplayText(status));
        mStatusBadgeCard.setCardBackgroundColor(
                ContextCompat.getColor(this, EventStatusUtil.toColorRes(status)));

        int rsvpCount = 0;
        if (event.getAttendeeIds() != null) rsvpCount = event.getAttendeeIds().size();
        else rsvpCount = Math.max(event.getRsvpCount(), 0);
        mTvTotalRsvps.setText(String.valueOf(rsvpCount));
    }

    private void loadAttendees(Event event) {
        List<String> ids = event.getAttendeeIds();
        if (ids == null || ids.isEmpty()) {
            mTvEmptyAttendees.setVisibility(android.view.View.VISIBLE);
            mAttendeeAdapter.updateData(new ArrayList<>());
            return;
        }

        mTvEmptyAttendees.setVisibility(android.view.View.GONE);
        mFriendService.fetchUsersByIds(
                ids,
                users -> mAttendeeAdapter.updateData(users),
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
