package com.example.cosmos_discovery.ui.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;

public class EventsAnalyticsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_analytics);

        // Setup Top Bar
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup Header Actions
        View eventHeader = findViewById(R.id.eventHeaderView);
        if (eventHeader != null) {
            Button btnAnnouncement = eventHeader.findViewById(R.id.btnAnnouncement);
            Button btnAttendeeList = eventHeader.findViewById(R.id.btnAttendeeList);

            if (btnAnnouncement != null) {
                btnAnnouncement.setOnClickListener(v -> {
                    Toast.makeText(this, "Make Announcement", Toast.LENGTH_SHORT).show();
                });
            }

            if (btnAttendeeList != null) {
                btnAttendeeList.setOnClickListener(v -> {
                    Toast.makeText(this, "View Attendee List", Toast.LENGTH_SHORT).show();
                });
            }
        }

        // Setup Bottom Nav interactions (Placeholder for future routing)
        View navHome = findViewById(R.id.navHome);
        View navMyEvents = findViewById(R.id.navMyEvents);
        View navFriends = findViewById(R.id.navFriends);

        if (navHome != null) {
            navHome.setOnClickListener(v -> finish());
        }
    }
}
