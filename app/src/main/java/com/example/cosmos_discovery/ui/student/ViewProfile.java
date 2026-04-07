package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Toast;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.ui.student.FriendsListActivity;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.EventSorter;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewProfile extends AppCompatActivity {

    private TextView     tvName, tvMajor, tvBio, tvBatch, tvFriendsCount, tvEventsCount;
    private TextView     tvNoUpcomingEvents;
    private LinearLayout llUpcomingEvents, bioContainer;

    private final FriendService friendService = new FriendService();
    private final EventService  eventService  = new EventService();
    private ListenerRegistration eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile); // your XML

        // Bind views
        tvName             = findViewById(R.id.tvName);
        tvMajor            = findViewById(R.id.tvMajor);
        tvBio              = findViewById(R.id.tvBio);
        tvBatch            = findViewById(R.id.tvBatch);
        tvFriendsCount     = findViewById(R.id.tvFriendsCount);
        tvEventsCount      = findViewById(R.id.tvEventsCount);
        tvNoUpcomingEvents = findViewById(R.id.tvNoUpcomingEvents);
        llUpcomingEvents   = findViewById(R.id.llUpcomingEvents);
        bioContainer       = findViewById(R.id.bioContainer);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get user
        User user = RoleUtil.getCurrentUser();

        // Populate UI
        if (user != null) {
            tvName.setText(user.getName() != null ? user.getName() : "");
            tvMajor.setText(user.getMajor() != null ? user.getMajor() : "");
            String bio = user.getBio();
            if (bio != null && !bio.trim().isEmpty()) {
                tvBio.setText(bio);
                bioContainer.setVisibility(View.VISIBLE);
            } else {
                bioContainer.setVisibility(View.GONE);
            }
            String email = user.getEmail();

            String batchText = "";

            if (email != null && email.length() >= 2 && Character.isDigit(email.charAt(0))) {
                String yearPrefix = email.substring(0, 2);
                batchText = "Batch " + "20" + yearPrefix;
            }

            tvBatch.setText(batchText);
            String uid = user.getUid();

            friendService.fetchFriends(uid,
                    entries -> {
                        int totalFriends = entries.size();
                        tvFriendsCount.setText(String.valueOf(totalFriends));
                    },
                    err -> {
                        Toast.makeText(ViewProfile.this, err, Toast.LENGTH_SHORT).show();
                    }
            );

            findViewById(R.id.cardFriends).setOnClickListener(v -> {
                Intent intent = new Intent(ViewProfile.this, FriendsListActivity.class);
                intent.putExtra(FriendsListActivity.EXTRA_USER_ID, uid);
                intent.putExtra(FriendsListActivity.EXTRA_TITLE, "My Friends");
                startActivity(intent);
            });

            eventListener = eventService.listenMyRsvpedEvents(uid,
                    events -> {
                        long now = System.currentTimeMillis();
                        List<Event> upcomingEvents = EventSorter.upcoming(events, now);
                        tvEventsCount.setText(events.size() + " Events Attended");
                        populateUpcomingEvents(upcomingEvents);
                    },
                    err -> {}
            );
        }

        findViewById(R.id.btnEdtProfile).setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfile.this, EditProfile.class);
            startActivity(intent);
        });
    }

    private void populateUpcomingEvents(List<Event> events) {
        llUpcomingEvents.removeAllViews();

        if (events.isEmpty()) {
            tvNoUpcomingEvents.setVisibility(View.VISIBLE);
            return;
        }

        tvNoUpcomingEvents.setVisibility(View.GONE);
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault());

        for (Event event : events) {
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.component_upcoming_event_row, llUpcomingEvents, false);
            ((TextView) row.findViewById(R.id.tvEventTitle)).setText(event.getTitle());
            ((TextView) row.findViewById(R.id.tvEventDate)).setText(
                    fmt.format(new Date(event.getDateTime())));
            llUpcomingEvents.addView(row);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }
}