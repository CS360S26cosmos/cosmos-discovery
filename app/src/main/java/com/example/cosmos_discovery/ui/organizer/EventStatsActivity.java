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
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.EventStats;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EventStatsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private List<Long> rsvpTimestamps = Collections.emptyList();
    private long eventCreatedAt = 0L;
    private long windowStart = 0L; // local-midnight epoch ms of leftmost day in 7-day window

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_stats);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        wireBottomNav();

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) return;

        EventService service = new EventService();
        service.fetchEventById(eventId,
                event -> runOnUiThread(() -> {
                    populateUI(event);
                    eventCreatedAt = event.getCreatedAt();
                    service.getRsvpTimestamps(eventId,
                            ts -> runOnUiThread(() -> {
                                rsvpTimestamps = ts;
                                initTimeline();
                            }),
                            err -> runOnUiThread(() -> {
                                rsvpTimestamps = Collections.emptyList();
                                initTimeline();
                            }));
                }),
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

    // ── Timeline (RSVPs per day) ────────────────────────────────────────

    private void initTimeline() {
        View card = findViewById(R.id.graphTimeline);
        if (card == null) return;

        // Default window = the 7-day window ending today (local time).
        windowStart = startOfDay(System.currentTimeMillis()) - 6 * DAY_MS;

        ImageView prev = card.findViewById(R.id.btnPrevWeek);
        ImageView next = card.findViewById(R.id.btnNextWeek);
        if (prev != null) prev.setOnClickListener(v -> shiftWindow(-7));
        if (next != null) next.setOnClickListener(v -> shiftWindow(+7));

        configureChart(card.findViewById(R.id.chartTimeline));
        renderTimeline();
    }

    private void shiftWindow(int days) {
        windowStart += days * DAY_MS;
        renderTimeline();
    }

    private void renderTimeline() {
        View card = findViewById(R.id.graphTimeline);
        if (card == null) return;
        BarChart chart = card.findViewById(R.id.chartTimeline);
        TextView label = card.findViewById(R.id.tvWeekLabel);
        TextView footnote = card.findViewById(R.id.tvTimelineFootnote);
        ImageView prev = card.findViewById(R.id.btnPrevWeek);
        ImageView next = card.findViewById(R.id.btnNextWeek);

        long windowEnd = windowStart + 7 * DAY_MS;

        // Bucket cached timestamps into 7 day-counts.
        int[] counts = new int[7];
        for (long ts : rsvpTimestamps) {
            if (ts < windowStart || ts >= windowEnd) continue;
            int bucket = (int) ((ts - windowStart) / DAY_MS);
            if (bucket >= 0 && bucket < 7) counts[bucket]++;
        }

        // Build chart entries.
        List<BarEntry> entries = new ArrayList<>(7);
        List<String> labels = new ArrayList<>(7);
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, counts[i]));
            labels.add(dayFmt.format(windowStart + i * DAY_MS));
        }
        BarDataSet ds = new BarDataSet(entries, "RSVPs");
        ds.setColor(ContextCompat.getColor(this, R.color.color_stats_accent));
        ds.setDrawValues(true);
        ds.setValueTextSize(10f);
        ds.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return value > 0 ? String.valueOf((int) value) : "";
            }
        });

        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);
        chart.setData(data);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.notifyDataSetChanged();
        chart.invalidate();

        // Update label.
        SimpleDateFormat rangeFmt = new SimpleDateFormat("MMM d", Locale.getDefault());
        if (label != null) {
            label.setText(rangeFmt.format(windowStart) + " – "
                    + rangeFmt.format(windowEnd - DAY_MS));
        }

        // Disable chevrons at boundaries.
        long createdDay = startOfDay(eventCreatedAt);
        long todayStart = startOfDay(System.currentTimeMillis());
        if (prev != null) {
            boolean canPrev = windowStart > createdDay;
            prev.setEnabled(canPrev);
            prev.setAlpha(canPrev ? 1f : 0.3f);
        }
        if (next != null) {
            boolean canNext = windowEnd <= todayStart + DAY_MS && windowEnd - DAY_MS < todayStart;
            next.setEnabled(canNext);
            next.setAlpha(canNext ? 1f : 0.3f);
        }

        // Footnote: show only if any cached RSVP timestamp predates the active window
        // and overlaps the backfill range (heuristic: window starts before today - 30d).
        if (footnote != null) {
            footnote.setVisibility(
                    windowStart < startOfDay(System.currentTimeMillis() - 30L * DAY_MS)
                            ? View.VISIBLE : View.GONE);
        }
    }

    private void configureChart(BarChart chart) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setExtraBottomOffset(6f);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setLabelCount(7, true);
        x.setTextColor(ContextCompat.getColor(this, R.color.color_stats_text));

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setGranularity(1f);
        left.setDrawGridLines(true);
        left.setTextColor(ContextCompat.getColor(this, R.color.color_stats_text));
        chart.getAxisRight().setEnabled(false);
    }

    private static long startOfDay(long epochMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMs);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
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
