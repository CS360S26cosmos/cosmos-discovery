package com.example.cosmos_discovery.ui.organizer;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;

import java.util.ArrayList;
import java.util.List;

public class EventsAnalyticsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private EventService mEventService;
    private String mEventId;
    private Event mEvent;

    private TextView tvCapacity, tvRsvps;
    private LineChart lineChart;
    private ScatterChart scatterChart;
    private Button btnAnnouncement, btnAttendeeList;
    private View btnBack, navHome, navMyEvents, navFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_analytics);

        mEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        // For demonstration, if eventId is null, we use a placeholder or handle it
        if (mEventId == null) {
            // Toast.makeText(this, "No event ID provided", Toast.LENGTH_SHORT).show();
            // In a real app, we might finish(), but for now let's show mock data
        }

        mEventService = new EventService();
        initViews();
        setupCharts();
        
        if (mEventId != null) {
            loadEventData();
        }
    }

    private void initViews() {
        tvCapacity = findViewById(R.id.capacity_value);
        tvRsvps = findViewById(R.id.rsvp_value);

        lineChart = findViewById(R.id.lineChart);
        scatterChart = findViewById(R.id.scatterChart);

        btnAnnouncement = findViewById(R.id.btnAnnouncement);
        btnAttendeeList = findViewById(R.id.btnAttendeeList);

        // Top bar back button
        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Navbar buttons
        navHome = findViewById(R.id.navHome);
        navMyEvents = findViewById(R.id.navMyEvents);
        navFriends = findViewById(R.id.navFriends);

        if (navHome != null) navHome.setOnClickListener(v -> Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show());
        if (navMyEvents != null) navMyEvents.setOnClickListener(v -> Toast.makeText(this, "My Events", Toast.LENGTH_SHORT).show());
        if (navFriends != null) navFriends.setOnClickListener(v -> Toast.makeText(this, "Friends", Toast.LENGTH_SHORT).show());

        btnAnnouncement.setOnClickListener(v -> Toast.makeText(this, "Announcement Sent", Toast.LENGTH_SHORT).show());
        btnAttendeeList.setOnClickListener(v -> Toast.makeText(this, "Opening Attendee List", Toast.LENGTH_SHORT).show());
    }

    private void loadEventData() {
        mEventService.fetchEventById(mEventId, event -> {
            mEvent = event;
            updateUI();
        }, error -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        if (mEvent == null) return;
        tvRsvps.setText(String.format("%d Students", mEvent.getRsvpCount()));
        // Capacity is usually a fixed or separate field, using placeholder or model if available
    }

    private void setupCharts() {
        setupScatterChart();
        setupLineChart();
    }

    private void setupScatterChart() {
        List<Entry> entries = new ArrayList<>();
        // Mock data matching the screenshot's trend for Views vs RSVPs
        entries.add(new Entry(10, 50));
        entries.add(new Entry(25, 150));
        entries.add(new Entry(40, 300));
        entries.add(new Entry(55, 400));
        entries.add(new Entry(70, 600));
        entries.add(new Entry(85, 750));
        entries.add(new Entry(100, 900));

        ScatterDataSet scatterDataSet = new ScatterDataSet(entries, "Views vs RSVPs");
        scatterDataSet.setColor(Color.parseColor("#7B0F4F"));
        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        scatterDataSet.setScatterShapeSize(12f);
        scatterDataSet.setDrawValues(false);

        // Add a trend line by overlaying a line data set (MPAndroidChart allows CombinedChart, 
        // but for simplicity in this scatter chart we just show the points as in the screen)
        // Actually, the screenshot shows a solid line too. 
        // Let's just focus on the points for now as ScatterChart doesn't easily do both without CombinedChart.
        
        ScatterData data = new ScatterData(scatterDataSet);
        scatterChart.setData(data);

        configureChartAxes(scatterChart.getXAxis(), scatterChart.getAxisLeft(), scatterChart.getAxisRight());
        scatterChart.getDescription().setEnabled(false);
        scatterChart.getLegend().setEnabled(false);
        scatterChart.setBackgroundColor(Color.TRANSPARENT);
        scatterChart.setDrawGridBackground(false);
        scatterChart.animateX(1000);
        scatterChart.invalidate();
    }

    private void setupLineChart() {
        List<Entry> entries = new ArrayList<>();
        // Mock data matching the screenshot's trend for RSVPs vs Days
        entries.add(new Entry(1, 20));
        entries.add(new Entry(2, 45));
        entries.add(new Entry(3, 70));
        entries.add(new Entry(4, 85));
        entries.add(new Entry(5, 95));

        LineDataSet lineDataSet = new LineDataSet(entries, "RSVPs Trend");
        lineDataSet.setColor(Color.parseColor("#7B0F4F"));
        lineDataSet.setCircleColor(Color.parseColor("#7B0F4F"));
        lineDataSet.setLineWidth(3f);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawValues(false);
        
        // Gradient fill
        lineDataSet.setDrawFilled(true);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#C897C3"), Color.parseColor("#F6EBF5")}
        );
        lineDataSet.setFillDrawable(gradient);

        LineData data = new LineData(lineDataSet);
        lineChart.setData(data);

        configureChartAxes(lineChart.getXAxis(), lineChart.getAxisLeft(), lineChart.getAxisRight());
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private void configureChartAxes(XAxis xAxis, YAxis leftAxis, YAxis rightAxis) {
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setGranularity(1f);

        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextColor(Color.GRAY);

        rightAxis.setEnabled(false);
    }
}
