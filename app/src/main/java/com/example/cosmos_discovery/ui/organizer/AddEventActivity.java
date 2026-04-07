package com.example.cosmos_discovery.ui.organizer;

import android.net.Uri;
import android.content.Intent;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private final EventService mEventService = new EventService();

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

    private List<String> mCategories;
    private String       mBannerUrl       = "";
    private boolean      mBannerUploading = false;

    private ActivityResultLauncher<String> mPickBannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_events_page);

        bindViews();
        setupDescriptionCounter();
        setupDropdowns();
        setupDateTimePickers();
        setupBannerUpload();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnCancelCard).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitCard).setOnClickListener(v -> onSubmit());

        wireBottomNav();
    }

    private void bindViews() {
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
        // Make the dropdown fields behave like dropdowns (tap opens picker)
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
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            target.setText(sdf.format(new Date(selection)));
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showTimePicker(EditText target) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
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

    private void onSubmit() {
        if (mBannerUploading) {
            Toast.makeText(this, "Banner is still uploading…", Toast.LENGTH_SHORT).show();
            return;
        }
        if (RoleUtil.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            return;
        }

        String title = mEtName.getText().toString().trim();
        String venue = mEtVenue.getText().toString().trim();

        if (title.isEmpty()) {
            mEtName.setError("Name is required.");
            return;
        }
        if (venue.isEmpty()) {
            mEtVenue.setError("Venue is required.");
            return;
        }

        String dateText  = mEtDate.getText().toString().trim();
        String startText = mEtStartTime.getText().toString().trim();
        long dateTimeMs  = EventFormUtil.parseDateTimeMs(dateText, startText);
        if (dateTimeMs <= 0) {
            mEtDate.setError("Enter a valid date.");
            mEtStartTime.setError("Enter a valid start time.");
            Toast.makeText(this, "Could not parse date/time. Try formats like 2026-04-06 and 5:00pm.", Toast.LENGTH_LONG).show();
            return;
        }

        String category = mEtCategory.getText().toString().trim();
        List<String> tags = new ArrayList<>();
        if (!category.isEmpty()) tags.add(category);

        Event event = new Event(
                title,
                dateTimeMs,
                venue,
                tags,
                mBannerUrl.isEmpty() ? mEtBannerUrl.getText().toString().trim() : mBannerUrl,
                RoleUtil.getCurrentUser().getUid()
        );

        event.setDescription(mEtDescription.getText().toString().trim());
        event.setCategory(category);
        event.setDateOfEvent(dateText);
        event.setStartTime(startText);
        event.setEndTime(mEtEndTime.getText().toString().trim());
        event.setRegisterBy(mEtRegisterBy.getText().toString().trim());
        event.setAccessType(EventFormUtil.normalizeAccessType(mEtOpenTo.getText().toString()));

        String capacityStr = mEtCapacity.getText().toString().trim();
        if (!capacityStr.isEmpty()) {
            try {
                int cap = Integer.parseInt(capacityStr);
                if (cap > 0) event.setCapacity(cap);
            } catch (NumberFormatException ignored) {}
        }

        mEventService.addEvent(
                event,
                newId -> {
                    Toast.makeText(this, "Event created (pending approval).", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, EventDetailsActivity.class);
                    intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, newId);
                    startActivity(intent);
                    finish();
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
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
