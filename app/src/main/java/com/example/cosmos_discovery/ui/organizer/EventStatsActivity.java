package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.EventStats;
import com.example.cosmos_discovery.ui.student.StudentActivity;

public class EventStatsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_stats);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        wireBottomNav();

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) return;

        new EventService().fetchEventById(eventId,
                event -> runOnUiThread(() -> populateUI(event)),
                err -> runOnUiThread(() ->
                        Toast.makeText(this, "Could not load event stats.", Toast.LENGTH_SHORT).show()));
    }

    private void populateUI(Event event) {
        EventStats stats = EventStats.compute(event);

        // Header — image + title
        View header = findViewById(R.id.eventHeaderView);
        if (header != null) {
            ImageView image = header.findViewById(R.id.imageViewEvent);
            TextView title = header.findViewById(R.id.textViewTitle);
            if (image != null)
                Glide.with(this)
                        .load(event.getImageUrl())
                        .placeholder(R.color.color_text_hint)
                        .centerCrop()
                        .into(image);
            if (title != null)
                title.setText(event.getTitle());
        }

        // Graph 1 — RSVP Fill Rate
        View graphCapacity = findViewById(R.id.graphCapacity);
        if (graphCapacity != null) {
            ProgressBar progress = graphCapacity.findViewById(R.id.progressRsvp);
            TextView tvRsvpLabel = graphCapacity.findViewById(R.id.tvRsvpFillLabel);
            TextView tvCapLabel  = graphCapacity.findViewById(R.id.tvCapacityLabel);
            TextView tvFillPercent = graphCapacity.findViewById(R.id.tvFillPercent);

            if (tvRsvpLabel != null) tvRsvpLabel.setText(stats.rsvpCount + " RSVPs");

            if (!stats.unlimitedCapacity) {
                if (progress != null) {
                    progress.setMax(100);
                    progress.setProgress(stats.fillRatePercent);
                    progress.setVisibility(View.VISIBLE);
                }
                if (tvCapLabel != null) tvCapLabel.setText("of " + stats.capacity + " spots");
                if (tvFillPercent != null) tvFillPercent.setText(stats.fillRatePercent + "%");
            } else {
                if (progress != null) progress.setVisibility(View.GONE);
                if (tvCapLabel != null) tvCapLabel.setText("Unlimited capacity");
            }
        }

        // Graph 2 — RSVPs vs Check-ins
        View graphCheckins = findViewById(R.id.graphCheckins);
        if (graphCheckins != null) {
            View barRsvp    = graphCheckins.findViewById(R.id.barRsvp);
            View barCheckin = graphCheckins.findViewById(R.id.barCheckin);
            TextView tvRsvpVal    = graphCheckins.findViewById(R.id.tvRsvpVal);
            TextView tvCheckinVal = graphCheckins.findViewById(R.id.tvCheckinVal);

            if (tvRsvpVal != null)    tvRsvpVal.setText(String.valueOf(stats.rsvpCount));
            if (tvCheckinVal != null) tvCheckinVal.setText(String.valueOf(stats.checkinCount));

            int maxBarPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
            int minBarPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

            setBarHeight(barRsvp,    Math.max(minBarPx, (int) (stats.rsvpBarFraction    * maxBarPx)));
            setBarHeight(barCheckin, Math.max(minBarPx, (int) (stats.checkinBarFraction * maxBarPx)));
        }
    }

    private void setBarHeight(View bar, int heightPx) {
        if (bar == null) return;
        ViewGroup.LayoutParams lp = bar.getLayoutParams();
        lp.height = heightPx;
        bar.setLayoutParams(lp);
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
