package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import android.content.Intent;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.ui.organizer.EventDetailsActivity;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.FriendEntry;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.EventSorter;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shows another user's profile: bio, friends count, events attended,
 * mutual friends (overlapping avatars), and upcoming RSVPed events.
 *
 * Launched with Intent extra {@code EXTRA_USER_ID} (String).
 */
public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";

    private final FriendService friendService = new FriendService();
    private final EventService  eventService  = new EventService();
    private ListenerRegistration eventListener;

    private TextView        tvName, tvBatch, tvMajor, tvBio;
    private TextView        tvFriendsCount, tvEventsCount;
    private TextView        tvMutualCount, tvNoUpcomingEvents, tvUpcomingEventsLabel;
    private View            layoutMutualFriends;
    private LinearLayout    llUpcomingEvents;
    private LinearLayout    bioContainer;
    private NestedScrollView nestedScrollView;
    private ShapeableImageView ivMutual1, ivMutual2, ivMutual3;
    private ImageView ivProfileImage;
    private MaterialButton btnAction;

    private String targetUid;
    private String currentUid;

    // Shared state between the two parallel fetchFriends calls
    private List<FriendEntry> targetFriends  = new ArrayList<>();
    private Set<String>       currentFriendUids = new HashSet<>();
    private final AtomicInteger fetchCount = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        targetUid  = getIntent().getStringExtra(EXTRA_USER_ID);
        User me = RoleUtil.getCurrentUser();
        if (me == null) { finish(); return; }
        currentUid = me.getUid();

        bindViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
    }

    private void bindViews() {
        tvName              = findViewById(R.id.tvName);
        tvBatch             = findViewById(R.id.tvBatch);
        tvMajor             = findViewById(R.id.tvMajor);
        tvBio               = findViewById(R.id.tvBio);
        bioContainer        = findViewById(R.id.bioContainer);
        tvFriendsCount      = findViewById(R.id.tvFriendsCount);
        tvEventsCount       = findViewById(R.id.tvEventsCount);
        tvMutualCount       = findViewById(R.id.tvMutualCount);
        tvNoUpcomingEvents  = findViewById(R.id.tvNoUpcomingEvents);
        layoutMutualFriends = findViewById(R.id.layoutMutualFriends);
        llUpcomingEvents    = findViewById(R.id.llUpcomingEvents);
        ivMutual1           = findViewById(R.id.ivMutual1);
        ivMutual2           = findViewById(R.id.ivMutual2);
        ivMutual3           = findViewById(R.id.ivMutual3);
        ivProfileImage        = findViewById(R.id.ivProfileImage);
        btnAction             = findViewById(R.id.btnAction);
        tvUpcomingEventsLabel = findViewById(R.id.tvUpcomingEventsLabel);
        nestedScrollView      = findViewById(R.id.nestedScrollView);

// Friends card → open friends list screen
        findViewById(R.id.cardFriends).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, FriendsListActivity.class);
            intent.putExtra(FriendsListActivity.EXTRA_USER_ID, targetUid);
            String name = tvName.getText().toString();
            String title = name.isEmpty() ? "Friends" : name.split(" ")[0] + "'s Friends";
            intent.putExtra(FriendsListActivity.EXTRA_TITLE, title);
            startActivity(intent);
        });
    }

    private void loadData() {
        // 1. Fetch target user details
        friendService.fetchUserById(targetUid,
                this::populateUserInfo,
                err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());

        // 2. Fetch target's friends (count + mutual intersection)
        friendService.fetchFriends(targetUid,
                entries -> {
                    tvFriendsCount.setText(String.valueOf(entries.size()));
                    targetFriends = entries;
                    if (fetchCount.incrementAndGet() == 2) onBothFriendsFetched();
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());

        // 3. Fetch current user's friends (button state + mutual intersection)
        friendService.fetchFriends(currentUid,
                entries -> {
                    for (FriendEntry e : entries) currentFriendUids.add(e.getUid());
                    updateActionButton();
                    if (fetchCount.incrementAndGet() == 2) onBothFriendsFetched();
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());

        // 4. Listen to target's RSVPed events → show upcoming
        eventListener = eventService.listenMyRsvpedEvents(targetUid,
                events -> {
                    long now = System.currentTimeMillis();
                    List<Event> upcomingEvents = EventSorter.upcoming(events, now);
                    tvEventsCount.setText(String.valueOf(events.size()));
                    populateUpcomingEvents(upcomingEvents);
                },
                err -> {});
    }

    private void populateUserInfo(User user) {
        tvName.setText(user.getName() != null ? user.getName() : "");
        tvMajor.setText(user.getMajor() != null ? user.getMajor() : "");

        String bio = user.getBio();
        if (bio != null && !bio.trim().isEmpty()) {
            tvBio.setText(bio);
            bioContainer.setVisibility(View.VISIBLE);
        } else {
            bioContainer.setVisibility(View.GONE);
        }

        String batchText = "";
        String email = user.getEmail();
        if (email != null && email.length() >= 2 && Character.isDigit(email.charAt(0))) {
            batchText = "Batch 20" + email.substring(0, 2);
        }
        tvBatch.setText(batchText);

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_topbar_person_icon)
                    .centerCrop()
                    .into(ivProfileImage);
        }
    }

    private void updateActionButton() {
        if (currentFriendUids.contains(targetUid)) {
            btnAction.setText("Remove Friend");
            btnAction.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_primary)));
            btnAction.setTextColor(ContextCompat.getColor(this, R.color.color_text_on_primary));
            btnAction.setStrokeWidth(0);
            btnAction.setOnClickListener(v -> removeFriend());
        } else {
            btnAction.setText("Add Friend");
            btnAction.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btnAction.setTextColor(ContextCompat.getColor(this, R.color.color_text_secondary));
            btnAction.setStrokeWidthResource(R.dimen.btn_stroke_width);
            btnAction.setStrokeColorResource(R.color.color_text_hint);
            btnAction.setOnClickListener(v -> addFriend());
        }
    }

    private void addFriend() {
        User currentUser = RoleUtil.getCurrentUser();
        friendService.fetchUserById(targetUid,
                target -> {
                    friendService.addFriend(currentUser, target,
                            () -> {
                                currentFriendUids.add(targetUid);
                                updateActionButton();
                                int newCount = Integer.parseInt(tvFriendsCount.getText().toString()) + 1;
                                tvFriendsCount.setText(String.valueOf(newCount));
                            },
                            err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
    }

    private void removeFriend() {
        friendService.removeFriend(currentUid, targetUid,
                () -> {
                    currentFriendUids.remove(targetUid);
                    updateActionButton();
                    int newCount = Math.max(0, Integer.parseInt(tvFriendsCount.getText().toString()) - 1);
                    tvFriendsCount.setText(String.valueOf(newCount));
                    // Remove from mutual friends display
                    layoutMutualFriends.setVisibility(View.GONE);
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
    }

    /** Called once both fetchFriends callbacks have returned — safe to compute mutual friends. */
    private void onBothFriendsFetched() {
        List<FriendEntry> mutual = new ArrayList<>();
        for (FriendEntry entry : targetFriends) {
            if (currentFriendUids.contains(entry.getUid())) {
                mutual.add(entry);
            }
        }
        showMutualFriends(mutual);
    }

    private void showMutualFriends(List<FriendEntry> mutual) {
        if (mutual.isEmpty()) {
            layoutMutualFriends.setVisibility(View.GONE);
            return;
        }

        layoutMutualFriends.setVisibility(View.VISIBLE);

        ShapeableImageView[] avatars = {ivMutual1, ivMutual2, ivMutual3};
        for (int i = 0; i < avatars.length; i++) {
            if (i < mutual.size()) {
                avatars[i].setVisibility(View.VISIBLE);
                String photoUrl = mutual.get(i).getPhotoUrl();
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_topbar_person_icon)
                            .centerCrop()
                            .into(avatars[i]);
                } else {
                    avatars[i].setImageResource(R.drawable.ic_topbar_person_icon);
                }
            } else {
                avatars[i].setVisibility(View.GONE);
            }
        }

        int count = mutual.size();
        String label = count == 1 ? "1 mutual friend" : count + " mutual friends";
        tvMutualCount.setText(label);
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
            row.setOnClickListener(v -> {
                if (event.getId() == null) return;
                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            });
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
