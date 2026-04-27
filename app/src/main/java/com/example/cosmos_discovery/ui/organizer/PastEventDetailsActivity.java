package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

    private static final int MAX_CHARS = 700;

    private final EventService mEventService = new EventService();

    private String mEventId;
    private String mUserId;

    // Star rating
    private ImageButton[] mStars;
    private boolean mRatingLocked = false;
    private int mCurrentRating = -1;
    private TextView mTvRatingLabel;

    // Review overlay
    private View mReviewOverlay;
    private EditText mEtReviewText;
    private TextView mTvCharCount;
    private String mExistingReview = null;

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
        setupReviewButton();
        setupViewReviewsButton();
        setupReviewOverlay();
        fetchAndBind();
        loadExistingRating();
        loadExistingReview();
        loadAverageRating();
    }

    // ── Navigation ────────────────────────────────────────────────────────

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

    // ── Star rating ───────────────────────────────────────────────────────

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
        mCurrentRating = rating;
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

    // ── Review overlay ────────────────────────────────────────────────────

    private void setupViewReviewsButton() {
        findViewById(R.id.tvViewReviews).setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewReviewsActivity.class);
            intent.putExtra("event_id", mEventId);
            intent.putExtra("current_user_rating", mCurrentRating);
            intent.putExtra("current_user_review", mExistingReview);
            startActivity(intent);
        });
    }

    private void setupReviewButton() {
        findViewById(R.id.addReviewButtonCard).setOnClickListener(v -> {
            if (!mRatingLocked) {
                Toast.makeText(this, "Add rating first", Toast.LENGTH_SHORT).show();
                return;
            }
            showReviewOverlay();
        });
    }

    private void setupReviewOverlay() {
        mReviewOverlay = findViewById(R.id.reviewOverlay);
        mEtReviewText  = mReviewOverlay.findViewById(R.id.etReviewText);
        mTvCharCount   = mReviewOverlay.findViewById(R.id.tvCharCount);

        mEtReviewText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTvCharCount.setText(s.length() + "/" + MAX_CHARS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Backdrop tap dismisses the overlay
        mReviewOverlay.setOnClickListener(v -> dismissReviewOverlay());
        // Card tap is consumed so it doesn't bubble to the backdrop listener
        mReviewOverlay.findViewById(R.id.reviewCard).setOnClickListener(v -> {});

        mReviewOverlay.findViewById(R.id.btnCloseReview)
                .setOnClickListener(v -> dismissReviewOverlay());
        mReviewOverlay.findViewById(R.id.btnCancelReview)
                .setOnClickListener(v -> dismissReviewOverlay());
        mReviewOverlay.findViewById(R.id.btnAddReview)
                .setOnClickListener(v -> submitReview());
    }

    private void showReviewOverlay() {
        String prefill = mExistingReview != null ? mExistingReview : "";
        mEtReviewText.setText(prefill);
        mEtReviewText.setSelection(prefill.length());
        mTvCharCount.setText(prefill.length() + "/" + MAX_CHARS);

        mReviewOverlay.setVisibility(View.VISIBLE);
        mEtReviewText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEtReviewText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void dismissReviewOverlay() {
        mReviewOverlay.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEtReviewText.getWindowToken(), 0);
    }

    private void submitReview() {
        String text = mEtReviewText.getText().toString().trim();
        if (mUserId == null) {
            dismissReviewOverlay();
            return;
        }
        mEventService.saveReview(mEventId, mUserId, text,
                () -> {
                    mExistingReview = text;
                    dismissReviewOverlay();
                },
                err -> Toast.makeText(this, "Could not save review.", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadExistingReview() {
        if (mUserId == null) return;
        mEventService.fetchReview(mEventId, mUserId,
                review -> mExistingReview = review,
                err -> {}
        );
    }

    private void loadAverageRating() {
        TextView tvAvgRating = findViewById(R.id.tvAvgRatingValue);
        mEventService.fetchAverageRating(mEventId,
                avg -> {
                    if (avg == null) {
                        tvAvgRating.setText("No ratings yet");
                    } else {
                        tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f / 5", avg));
                    }
                },
                err -> {}
        );
    }

    // ── Event data ────────────────────────────────────────────────────────

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
                event.getDescription() != null && !event.getDescription().trim().isEmpty()
                        ? event.getDescription()
                        : "No description provided."
        );
    }
}
