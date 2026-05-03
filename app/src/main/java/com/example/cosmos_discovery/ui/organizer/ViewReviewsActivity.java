package com.example.cosmos_discovery.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Review;
import com.example.cosmos_discovery.util.RoleUtil;

import java.util.List;

public class ViewReviewsActivity extends AppCompatActivity {

    private final EventService mEventService = new EventService();

    private LinearLayout mContainer;
    private String mCurrentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reviews);

        String eventId = getIntent().getStringExtra("event_id");
        int    myRating = getIntent().getIntExtra("current_user_rating", -1);
        String myReview = getIntent().getStringExtra("current_user_review");

        mCurrentUserId = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid() : null;

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        mContainer = findViewById(R.id.reviewsContainer);

        // Current user's review pinned at the top
        boolean hasOwnReview = myRating > 0
                && myReview != null && !myReview.trim().isEmpty();
        if (hasOwnReview) {
            addSectionLabel("Your Review", false);
            addReviewCard("You", myRating, myReview);
        }

        loadOtherReviews(eventId, hasOwnReview);
    }

    private void loadOtherReviews(String eventId, boolean hasOwnReview) {
        if (eventId == null) return;

        mEventService.fetchAllReviews(eventId, reviews -> {
            boolean addedHeader = false;

            for (Review r : reviews) {
                if (r.userId.equals(mCurrentUserId)) continue;
                if (!r.text.trim().isEmpty()) {
                    if (!addedHeader) {
                        addSectionLabel("Other Reviews", hasOwnReview);
                        addedHeader = true;
                    }
                    addReviewCard(r.name, r.rating, r.text);
                }
            }

            if (mContainer.getChildCount() == 0) {
                addEmptyState("No reviews yet.");
            }

        }, err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show());
    }

    private void addSectionLabel(String text, boolean addTopMargin) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(17f);
        tv.setTextColor(getColor(R.color.color_text_secondary));
        tv.setTypeface(ResourcesCompat.getFont(this, R.font.inter_variable),
                android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        if (addTopMargin) lp.topMargin = dp(20);
        tv.setLayoutParams(lp);

        mContainer.addView(tv);
    }

    private void addReviewCard(String name, int rating, String text) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_review, mContainer, false);

        ((TextView) card.findViewById(R.id.tvReviewerName)).setText(name);
        ((TextView) card.findViewById(R.id.tvReviewerRating)).setText(rating + "/5");
        ((TextView) card.findViewById(R.id.tvReviewText)).setText(text);

        mContainer.addView(card);
    }

    private void addEmptyState(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(14f);
        tv.setTextColor(getColor(R.color.color_text_hint));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTypeface(ResourcesCompat.getFont(this, R.font.inter_variable),
                android.graphics.Typeface.NORMAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(48);
        tv.setLayoutParams(lp);

        mContainer.addView(tv);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
