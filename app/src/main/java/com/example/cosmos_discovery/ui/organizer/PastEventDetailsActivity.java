package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.RoleUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PastEventDetailsActivity extends AppCompatActivity {

    private final EventService mEventService = new EventService();

    private String mEventId;
    private String mUserId;
    private ImageButton[] mStars;
    private boolean mRatingLocked = false;
    private TextView mTvRatingLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pastevent_details_page);

        mEventId = getIntent().getStringExtra("event_id");

        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mUserId = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : null;

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupNavBar();
        setupStarRating();
        fetchAndBind();
        loadExistingRating();
    }

    private void setupNavBar() {
        ImageView iconHome     = findViewById(R.id.iconHome);
        ImageView iconMyEvents = findViewById(R.id.iconMyEvents);
        ImageView iconFriends  = findViewById(R.id.iconFriends);

        iconHome.setImageResource(R.drawable.ic_home_outline);
        iconMyEvents.setImageResource(R.drawable.ic_bookmark_selected);
        iconFriends.setImageResource(R.drawable.ic_heart_outline);

        LinearLayout navHome     = findViewById(R.id.navHome);
        LinearLayout navMyEvents = findViewById(R.id.navMyEvents);
        LinearLayout navFriends  = findViewById(R.id.navFriends);

        navMyEvents.setOnClickListener(v -> finish());
        navHome.setOnClickListener(v    -> navigateToStudentTab(StudentActivity.TAB_DISCOVER));
        navFriends.setOnClickListener(v -> navigateToStudentTab(StudentActivity.TAB_FRIENDS));
    }

    private void navigateToStudentTab(int tab) {
        Intent intent = new Intent(this, StudentActivity.class);
        intent.putExtra(StudentActivity.EXTRA_START_TAB, tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void setupStarRating() {
        mTvRatingLabel = findViewById(R.id.tvRatingLabel);

        mStars = new ImageButton[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        for (int i = 0; i < mStars.length; i++) {
            final int rating = i + 1;
            mStars[i].setOnClickListener(v -> {
                if (mRatingLocked) return;
                applyRating(rating);
                mRatingLocked = true;
                if (mUserId != null) {
                    mEventService.saveRating(mEventId, mUserId, rating,
                            () -> {},
                            err -> Toast.makeText(this, "Could not save rating.", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    private void applyRating(int rating) {
        for (int i = 0; i < mStars.length; i++) {
            mStars[i].setImageResource(i < rating ? R.drawable.star_filled : R.drawable.star_outline);
        }
        mTvRatingLabel.setText(rating + "/5");
    }

    private void loadExistingRating() {
        if (mUserId == null) return;
        mEventService.fetchRating(mEventId, mUserId,
                rating -> {
                    if (rating != null) {
                        applyRating(rating);
                        mRatingLocked = true;
                    }
                },
                err -> {}
        );
    }

    private void fetchAndBind() {
        mEventService.fetchEventById(
                mEventId,
                this::bindEvent,
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private void bindEvent(Event event) {
        ImageView image     = findViewById(R.id.imageViewEvent);
        TextView  title     = findViewById(R.id.textViewEventTitle);
        TextView  venue     = findViewById(R.id.tvVenueValue);
        TextView  dateTime  = findViewById(R.id.tvDateTimeValue);
        TextView  organizer = findViewById(R.id.tvOrganizerValue);
        TextView  desc      = findViewById(R.id.tvDescriptionValue);

        Glide.with(this)
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(image);

        title.setText(event.getTitle());
        venue.setText(event.getLocation());

        SimpleDateFormat sdf =
                new SimpleDateFormat("MMM d, yyyy | h:mma", Locale.getDefault());
        dateTime.setText(sdf.format(new Date(event.getDateTime())));

        String name = event.getOrganizerName();
        organizer.setText(name != null && !name.isEmpty() ? name : "Unknown");

        desc.setText(
                event.getDescription() != null &&
                        !event.getDescription().trim().isEmpty()
                        ? event.getDescription()
                        : "No description provided."
        );
    }
}
