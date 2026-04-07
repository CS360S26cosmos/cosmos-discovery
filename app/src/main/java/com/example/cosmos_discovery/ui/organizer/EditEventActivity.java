package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.EventFormUtil;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class EditEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private final EventService mEventService = new EventService();

    private String mEventId;
    private Event  mEvent;

    private EditText mEtName;
    private EditText mEtDescription;
    private TextView mTvDescriptionCounter;
    private EditText mEtCategory;
    private EditText mEtBannerUrl;
    private EditText mEtDate;
    private EditText mEtStartTime;
    private EditText mEtEndTime;
    private EditText mEtRegisterBy;
    private EditText mEtVenue;
    private EditText mEtCapacity;
    private EditText mEtOpenTo;

    private TextView mTvTitle;
    private TextView mTvSubmit;

    private List<String> mCategories;
    private String       mBannerUrl       = "";
    private boolean      mBannerUploading = false;
    private ActivityResultLauncher<String> mPickBannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_events_page);

        mEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupDescriptionCounter();
        setupDropdowns();
        setupDateTimePickers();
        setupBannerUpload();

        mTvTitle.setText("Edit Event");
        mTvSubmit.setText("Save Changes");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnCancelCard).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitCard).setOnClickListener(v -> onSave());

        wireBottomNav();
        fetchAndPrefill();
    }

    private void bindViews() {
        mTvTitle  = findViewById(R.id.tvAddEventTitle);
        mTvSubmit = findViewById(R.id.tvSubmit);

        mEtName               = findViewById(R.id.etEventName);
        mEtDescription        = findViewById(R.id.etDescription);
        mTvDescriptionCounter = findViewById(R.id.tvDescriptionCounter);
        mEtCategory           = findViewById(R.id.etCategory);
        mEtBannerUrl   = findViewById(R.id.etBannerUpload);
        mEtDate        = findViewById(R.id.etDateOfEvent);
        mEtStartTime   = findViewById(R.id.etStartTime);
        mEtEndTime     = findViewById(R.id.etEndTime);
        mEtRegisterBy  = findViewById(R.id.etRegisterBy);
        mEtVenue       = findViewById(R.id.etVenue);
        mEtCapacity    = findViewById(R.id.etCapacity);
        mEtOpenTo      = findViewById(R.id.etOpenTo);
    }

    private void setupDescriptionCounter() {
        mEtDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                mTvDescriptionCounter.setText(s.length() + " / 150");
            }
        });
    }

    private void setupDropdowns() {
        mEtCategory.setInputType(InputType.TYPE_NULL);
        mEtCategory.setKeyListener(null);
        mEtCategory.setOnClickListener(v -> showCategoryPicker());
        findViewById(R.id.iconCategory).setOnClickListener(v -> showCategoryPicker());

        mEtOpenTo.setInputType(InputType.TYPE_NULL);
        mEtOpenTo.setKeyListener(null);
        mEtOpenTo.setOnClickListener(v -> showOpenToPicker());
        findViewById(R.id.iconOpenTo).setOnClickListener(v -> showOpenToPicker());
    }

    private void setupDateTimePickers() {
        mEtDate.setInputType(InputType.TYPE_NULL);
        mEtDate.setKeyListener(null);
        mEtDate.setFocusable(false);
        mEtStartTime.setInputType(InputType.TYPE_NULL);
        mEtStartTime.setKeyListener(null);
        mEtStartTime.setFocusable(false);
        mEtEndTime.setInputType(InputType.TYPE_NULL);
        mEtEndTime.setKeyListener(null);
        mEtEndTime.setFocusable(false);
        mEtRegisterBy.setInputType(InputType.TYPE_NULL);
        mEtRegisterBy.setKeyListener(null);
        mEtRegisterBy.setFocusable(false);

        View.OnClickListener datePicker = v -> showDatePicker(mEtDate);
        mEtDate.setOnClickListener(datePicker);
        findViewById(R.id.iconDateOfEvent).setOnClickListener(datePicker);

        View.OnClickListener regByPicker = v -> showDatePicker(mEtRegisterBy);
        mEtRegisterBy.setOnClickListener(regByPicker);
        findViewById(R.id.iconRegisterBy).setOnClickListener(regByPicker);

        View.OnClickListener startPicker = v -> showTimePicker(mEtStartTime);
        mEtStartTime.setOnClickListener(startPicker);
        findViewById(R.id.iconStartTime).setOnClickListener(startPicker);

        View.OnClickListener endPicker = v -> showTimePicker(mEtEndTime);
        mEtEndTime.setOnClickListener(endPicker);
        findViewById(R.id.iconEndTime).setOnClickListener(endPicker);
    }

    private void showDatePicker(EditText target) {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setCalendarConstraints(constraints);
        String currentText = target.getText().toString().trim();
        if (!currentText.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date parsed = sdf.parse(currentText);
                if (parsed != null) {
                    builder.setSelection(parsed.getTime());
                }
            } catch (Exception ignored) {}
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            target.setText(sdf.format(new Date(selection)));
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showTimePicker(EditText target) {
        int initHour = 12;
        int initMinute = 0;
        String currentText = target.getText().toString().trim();
        if (!currentText.isEmpty()) {
            try {
                boolean isPm = currentText.endsWith("pm");
                String timePart = currentText.substring(0, currentText.length() - 2);
                String[] parts = timePart.split(":");
                int h12 = Integer.parseInt(parts[0]);
                int mins = Integer.parseInt(parts[1]);
                initHour   = isPm ? (h12 == 12 ? 12 : h12 + 12) : (h12 == 12 ? 0 : h12);
                initMinute = mins;
            } catch (Exception ignored) {}
        }
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initHour)
                .setMinute(initMinute)
                .setTitleText("Select time")
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int h = picker.getHour();
            int m = picker.getMinute();
            String amPm = h < 12 ? "am" : "pm";
            int h12 = h == 0 ? 12 : (h > 12 ? h - 12 : h);
            target.setText(String.format(Locale.US, "%d:%02d%s", h12, m, amPm));
        });
        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void showCategoryPicker() {
        if (mCategories == null) {
            mEventService.fetchCategories(
                    names -> {
                        mCategories = names;
                        showCategoryPicker();
                    },
                    err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            );
            return;
        }
        if (mCategories.isEmpty()) {
            Toast.makeText(this, "No categories found.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = mCategories.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select category")
                .setItems(items, (d, which) -> mEtCategory.setText(items[which]))
                .show();
    }

    private void showOpenToPicker() {
        String[] items = new String[]{"LUMS Only", "Open"};
        new AlertDialog.Builder(this)
                .setTitle("Open to")
                .setItems(items, (d, which) -> mEtOpenTo.setText(items[which]))
                .show();
    }

    private void setupBannerUpload() {
        mEtBannerUrl.setInputType(InputType.TYPE_NULL);
        mEtBannerUrl.setKeyListener(null);

        mPickBannerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadBanner(uri);
                }
        );
        mEtBannerUrl.setOnClickListener(v -> mPickBannerLauncher.launch("image/*"));
        findViewById(R.id.iconBannerUpload).setOnClickListener(v -> mPickBannerLauncher.launch("image/*"));
    }

    private void uploadBanner(Uri uri) {
        if (RoleUtil.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            return;
        }

        mBannerUploading = true;
        Toast.makeText(this, "Uploading banner…", Toast.LENGTH_SHORT).show();

        String uid = RoleUtil.getCurrentUser().getUid();
        String filename = String.format(Locale.US, "event_banners/%s/%d.jpg", uid, System.currentTimeMillis());
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filename);

        ref.putFile(uri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            mBannerUploading = false;
                            mBannerUrl = downloadUri.toString();
                            mEtBannerUrl.setText("banner_image.jpg");
                            Toast.makeText(this, "Banner uploaded.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            mBannerUploading = false;
                            Toast.makeText(this, "Could not get banner URL.", Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    mBannerUploading = false;
                    Toast.makeText(this, "Banner upload failed.", Toast.LENGTH_LONG).show();
                });
    }

    private void fetchAndPrefill() {
        mEventService.fetchEventById(
                mEventId,
                event -> {
                    mEvent = event;
                    prefill(event);
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private void prefill(Event event) {
        mEtName.setText(nullToEmpty(event.getTitle()));
        mEtDescription.setText(nullToEmpty(event.getDescription()));
        mTvDescriptionCounter.setText(mEtDescription.getText().length() + " / 150");

        String category = event.getCategory();
        if (category == null || category.trim().isEmpty()) {
            if (event.getTags() != null && !event.getTags().isEmpty()) category = event.getTags().get(0);
        }
        mEtCategory.setText(nullToEmpty(category));

        String imageUrl = nullToEmpty(event.getImageUrl());
        mBannerUrl = imageUrl;
        mEtBannerUrl.setText(imageUrl.isEmpty() ? "" : "banner_image.jpg");

        String dateText = event.getDateOfEvent();
        if (dateText == null || dateText.trim().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateText = sdf.format(new Date(event.getDateTime()));
        }
        mEtDate.setText(dateText);

        mEtStartTime.setText(nullToEmpty(event.getStartTime()));
        mEtEndTime.setText(nullToEmpty(event.getEndTime()));
        mEtRegisterBy.setText(nullToEmpty(event.getRegisterBy()));
        mEtVenue.setText(nullToEmpty(event.getLocation()));
        mEtOpenTo.setText(EventFormUtil.accessTypeToDisplay(event.getAccessType()));
        if (event.getCapacity() > 0) {
            mEtCapacity.setText(String.valueOf(event.getCapacity()));
        }
    }

    private void onSave() {
        if (mEvent == null) {
            Toast.makeText(this, "Still loading event…", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mBannerUploading) {
            Toast.makeText(this, "Banner is still uploading…", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = mEtName.getText().toString().trim();
        String venue = mEtVenue.getText().toString().trim();
        String dateText  = mEtDate.getText().toString().trim();
        String startText = mEtStartTime.getText().toString().trim();

        boolean hasError = false;

        if (title.isEmpty()) {
            mEtName.setError("Event name is required.");
            hasError = true;
        }
        if (venue.isEmpty()) {
            mEtVenue.setError("Venue is required.");
            hasError = true;
        }
        if (dateText.isEmpty()) {
            mEtDate.setError("Date is required.");
            hasError = true;
        }
        if (startText.isEmpty()) {
            mEtStartTime.setError("Start time is required.");
            hasError = true;
        }

        if (hasError) return;

        long dateTimeMs = EventFormUtil.parseDateTimeMs(dateText, startText);
        if (dateTimeMs <= 0) {
            mEtDate.setError("Enter a valid date.");
            mEtStartTime.setError("Enter a valid start time.");
            Toast.makeText(this, "Could not parse date/time.", Toast.LENGTH_LONG).show();
            return;
        }
        if (dateTimeMs < System.currentTimeMillis()) {
            mEtDate.setError("Event date cannot be in the past.");
            return;
        }

        String endText = mEtEndTime.getText().toString().trim();
        if (!endText.isEmpty()) {
            long endTimeMs = EventFormUtil.parseDateTimeMs(dateText, endText);
            if (endTimeMs > 0 && endTimeMs <= dateTimeMs) {
                mEtEndTime.setError("End time must be after start time.");
                return;
            }
        }

        String regByText = mEtRegisterBy.getText().toString().trim();
        if (!regByText.isEmpty()) {
            long regByMs = EventFormUtil.parseDateTimeMs(regByText, "11:59pm");
            if (regByMs > 0 && regByMs < System.currentTimeMillis()) {
                mEtRegisterBy.setError("Registration deadline cannot be in the past.");
                return;
            }
            if (regByMs > 0 && regByMs > dateTimeMs) {
                mEtRegisterBy.setError("Registration deadline must be before the event date.");
                return;
            }
        }

        String category = mEtCategory.getText().toString().trim();
        List<String> tags = new ArrayList<>();
        if (!category.isEmpty()) tags.add(category);

        Map<String, Object> fields = new HashMap<>();
        fields.put("title", title);
        fields.put("location", venue);
        fields.put("dateTime", dateTimeMs);
        fields.put("imageUrl", mBannerUrl);
        fields.put("description", mEtDescription.getText().toString().trim());
        fields.put("category", category);
        fields.put("dateOfEvent", dateText);
        fields.put("startTime", startText);
        fields.put("endTime", mEtEndTime.getText().toString().trim());
        fields.put("registerBy", mEtRegisterBy.getText().toString().trim());
        fields.put("accessType", EventFormUtil.normalizeAccessType(mEtOpenTo.getText().toString()));
        fields.put("tags", tags);

        String capacityStr = mEtCapacity.getText().toString().trim();
        int cap = 0;
        if (!capacityStr.isEmpty()) {
            try { cap = Integer.parseInt(capacityStr); } catch (NumberFormatException ignored) {}
        }
        fields.put("capacity", cap);

        mEventService.updateEvent(
                mEventId,
                fields,
                () -> {
                    Toast.makeText(this, "Event updated.", Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
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
