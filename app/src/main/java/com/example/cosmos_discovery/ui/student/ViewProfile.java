package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Toast;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.EventSorter;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.cosmos_discovery.model.Event;

import java.util.List;

public class ViewProfile extends AppCompatActivity {

    private TextView tvName, tvMajor, tvBio, tvBatch, tvFriendsCount;

    private final FriendService friendService = new FriendService();
    private TextView tvEventsCount;
    private final EventService eventService = new EventService();
    private ListenerRegistration eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile); // your XML

        // Bind views
        tvName  = findViewById(R.id.tvName);
        tvMajor = findViewById(R.id.tvMajor);
        tvBio   = findViewById(R.id.tvBio);
        tvBatch = findViewById(R.id.tvBatch);
        tvFriendsCount = findViewById(R.id.tvFriendsCount);
        tvEventsCount = findViewById(R.id.tvEventsCount);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get user
        User user = RoleUtil.getCurrentUser();

        // Populate UI
        if (user != null) {
            tvName.setText(user.getName() != null ? user.getName() : "");
            tvMajor.setText(user.getMajor() != null ? user.getMajor() : "");
            tvBio.setText(user.getBio() != null ? user.getBio() : "");
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

            eventListener = eventService.listenMyRsvpedEvents(uid,
                    events -> {
                        long now = System.currentTimeMillis();

                        // same logic as MyEventsFragment
                        List<Event> pastEvents = EventSorter.past(events, now);

                        tvEventsCount.setText(String.valueOf(pastEvents.size()));
                    },
                    err -> {}
            );
        }

        findViewById(R.id.btnEdtProfile).setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfile.this, EditProfile.class);
            startActivity(intent);
        });
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