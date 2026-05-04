package com.example.cosmos_discovery.ui.admin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AdminService;
import com.example.cosmos_discovery.database.RsvpBackfillMigration;
import com.example.cosmos_discovery.model.Event;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private static final String DATE_DISPLAY_FORMAT = "MMM d, yyyy";
    private static final String DATE_FILE_FORMAT    = "yyyy-MM-dd";

    // ── Date range filter state ────────────────────────────────────────────
    private long mFromMs = 0;
    private long mToMs   = Long.MAX_VALUE;

    // ── Cached events list for CSV export ─────────────────────────────────
    private List<Event> mAllEvents;

    // ── Views ──────────────────────────────────────────────────────────────
    private TextView     mBtnFrom;
    private TextView     mBtnTo;
    private TextView     mBtnClearFilter;
    private TextView     mTextActive;
    private TextView     mTextPending;
    private TextView     mTextPast;
    private TextView     mTextRsvps;
    private TextView     mTextUsers;
    private View         mProgressBar;
    private BarChartView mBarChart;

    private final AdminService mAdminService = new AdminService();

    // ── Storage permission launcher (API < 29 only) ────────────────────────
    private ActivityResultLauncher<String> mStoragePermLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStoragePermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        doExport();
                    } else {
                        Toast.makeText(requireContext(),
                                "Storage permission required to export CSV",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners(view);
        loadMetrics();
    }

    // ── View binding ───────────────────────────────────────────────────────

    private void bindViews(View root) {
        mBtnFrom        = root.findViewById(R.id.btnFrom);
        mBtnTo          = root.findViewById(R.id.btnTo);
        mBtnClearFilter = root.findViewById(R.id.btnClearFilter);
        mTextActive     = root.findViewById(R.id.textActiveCount);
        mTextPending    = root.findViewById(R.id.textPendingCount);
        mTextPast       = root.findViewById(R.id.textPastCount);
        mTextRsvps      = root.findViewById(R.id.textRsvpCount);
        mTextUsers      = root.findViewById(R.id.textUserCount);
        mProgressBar    = root.findViewById(R.id.progressBar);
        mBarChart       = root.findViewById(R.id.barChartRsvps);
    }

    private void setupClickListeners(View root) {
        mBtnFrom.setOnClickListener(v -> showDatePicker(true));
        mBtnTo.setOnClickListener(v -> showDatePicker(false));
        mBtnClearFilter.setOnClickListener(v -> clearFilter());
        root.findViewById(R.id.btnExport).setOnClickListener(v -> exportCsv());

        // Hidden admin-only trigger: long-press the user count to run the
        // one-time RSVP timestamp backfill migration. Idempotent.
        if (mTextUsers != null) {
            mTextUsers.setOnLongClickListener(v -> {
                runRsvpBackfill();
                return true;
            });
        }
    }

    private void runRsvpBackfill() {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "RSVP backfill starting…", Toast.LENGTH_SHORT).show();
        new RsvpBackfillMigration().run(
                msg -> { if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show(); },
                err -> { if (isAdded()) Toast.makeText(requireContext(),
                        "Backfill failed: " + err, Toast.LENGTH_LONG).show(); });
    }

    // ── Metrics loading ────────────────────────────────────────────────────

    private void loadMetrics() {
        mProgressBar.setVisibility(View.VISIBLE);

        mAdminService.fetchDashboardMetrics(mFromMs, mToMs,
                events -> {
                    mAllEvents = events;
                    updateChart(events);
                },
                metrics -> {
                    if (!isAdded()) return;
                    mProgressBar.setVisibility(View.GONE);
                    mTextActive.setText(String.valueOf(metrics.activeEvents));
                    mTextPending.setText(String.valueOf(metrics.pendingEvents));
                    mTextPast.setText(String.valueOf(metrics.recentEvents));
                    mTextRsvps.setText(String.valueOf(metrics.totalRsvps));
                    mTextUsers.setText(String.valueOf(metrics.totalUsers));
                },
                error -> {
                    if (!isAdded()) return;
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
    }

    // ── Bar chart ──────────────────────────────────────────────────────────

    private void updateChart(List<Event> allEvents) {
        if (!isAdded() || mBarChart == null) return;

        long now          = System.currentTimeMillis();
        long thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000;

        // Keep only events created in the last 30 days
        List<Event> recent = new ArrayList<>();
        for (Event e : allEvents) {
            long ca = e.getCreatedAt();
            if (ca > 0 && ca >= thirtyDaysAgo) recent.add(e);
        }

        // Sort by rsvpCount descending, cap at 7 bars
        recent.sort((a, b) -> Integer.compare(b.getRsvpCount(), a.getRsvpCount()));
        if (recent.size() > 7) recent = recent.subList(0, 7);

        List<BarChartView.BarEntry> entries = new ArrayList<>();
        for (Event e : recent) {
            String title = e.getTitle() != null ? e.getTitle() : "Untitled";
            entries.add(new BarChartView.BarEntry(title, e.getRsvpCount()));
        }

        mBarChart.setEntries(entries);
    }

    // ── Date range picker ──────────────────────────────────────────────────

    private void showDatePicker(boolean isFrom) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isFrom ? "Select start date" : "Select end date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtcMs -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_DISPLAY_FORMAT, Locale.getDefault());
            String label = sdf.format(new Date(selectionUtcMs));
            if (isFrom) {
                mFromMs = selectionUtcMs;
                mBtnFrom.setText("From: " + label);
            } else {
                mToMs = selectionUtcMs + 86_400_000L - 1; // end of selected day (23:59:59.999)
                mBtnTo.setText("To: " + label);
            }
            mBtnClearFilter.setVisibility(View.VISIBLE);
            loadMetrics();
        });

        picker.show(getChildFragmentManager(), "date_picker");
    }

    private void clearFilter() {
        mFromMs = 0;
        mToMs   = Long.MAX_VALUE;
        mBtnFrom.setText("From: All time");
        mBtnTo.setText("To: All time");
        mBtnClearFilter.setVisibility(View.GONE);
        loadMetrics();
    }

    // ── CSV export ─────────────────────────────────────────────────────────

    private void exportCsv() {
        if (mAllEvents == null) {
            Toast.makeText(requireContext(), "No data loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mStoragePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        doExport();
    }

    private void doExport() {
        String fileName = "cosmos_metrics_"
                + new SimpleDateFormat(DATE_FILE_FORMAT, Locale.getDefault()).format(new Date())
                + ".csv";
        String csv = buildCsv(mAllEvents);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(fileName, csv);
            } else {
                saveViaLegacy(fileName, csv);
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Saves the CSV using MediaStore.Downloads (Android 10+, no permission needed). */
    private void saveViaMediaStore(String fileName, String csv) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        ContentResolver resolver = requireContext().getContentResolver();
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Could not create Downloads entry");

        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) throw new IOException("Could not open output stream");
            out.write(csv.getBytes(StandardCharsets.UTF_8));
        }

        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);

        Toast.makeText(requireContext(),
                "Saved to Downloads/" + fileName, Toast.LENGTH_LONG).show();
    }

    /** Saves the CSV to the public Downloads directory (Android 9 and below). */
    private void saveViaLegacy(String fileName, String csv) throws IOException {
        File dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(csv);
        }
        Toast.makeText(requireContext(),
                "Saved to Downloads/" + fileName, Toast.LENGTH_LONG).show();
    }

    /** Builds a CSV string from events in the current date range filter. */
    private String buildCsv(List<Event> events) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FILE_FORMAT, Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append("Title,Status,Date,Category,RSVP Count,Organizer\n");
        for (Event e : events) {
            long dt = e.getDateTime();
            if (dt < mFromMs || dt > mToMs) continue;
            sb.append(escapeCsv(e.getTitle())).append(',');
            sb.append(escapeCsv(e.getStatus())).append(',');
            sb.append(sdf.format(new Date(dt))).append(',');
            sb.append(escapeCsv(e.getCategory())).append(',');
            sb.append(e.getRsvpCount()).append(',');
            sb.append(escapeCsv(e.getOrganizerName())).append('\n');
        }
        return sb.toString();
    }

    /** Wraps a CSV field in quotes if it contains commas, quotes, or newlines. */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
