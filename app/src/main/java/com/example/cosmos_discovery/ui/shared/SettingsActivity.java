package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.database.PreferenceService;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private final AuthService        mAuthService = new AuthService();
    private final PreferenceService  mPrefService = new PreferenceService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvResetPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        // Stubs (notifications no longer stubbed — handled below)
        int[] stubs = { R.id.rowHelpCenter, R.id.rowContactUs, R.id.rowTerms };
        for (int id : stubs) {
            findViewById(id).setOnClickListener(v ->
                    Toast.makeText(this, "Will be implemented soon", Toast.LENGTH_SHORT).show());
        }

        setupOrganizerRequestRow();
        setupNotificationToggles();
    }

    private void setupNotificationToggles() {
        SwitchMaterial swMaster        = findViewById(R.id.swPushMaster);
        SwitchMaterial swRsvp          = findViewById(R.id.swPrefRsvp);
        SwitchMaterial swEventUpdates  = findViewById(R.id.swPrefEventUpdates);
        SwitchMaterial swAnnouncements = findViewById(R.id.swPrefAnnouncements);
        SwitchMaterial swFriends       = findViewById(R.id.swPrefFriendRequests);
        SwitchMaterial swAdminDecs     = findViewById(R.id.swPrefAdminDecisions);
        SwitchMaterial swCapacity      = findViewById(R.id.swPrefCapacityFull);
        SwitchMaterial swRsvpReceived  = findViewById(R.id.swPrefRsvpReceived);

        if (RoleUtil.isOrganizer()) {
            swAdminDecs   .setVisibility(View.VISIBLE);
            swCapacity    .setVisibility(View.VISIBLE);
            swRsvpReceived.setVisibility(View.VISIBLE);
        }

        User user = RoleUtil.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        // Disable interaction until prefs load to avoid stale-write races.
        SwitchMaterial[] all = { swMaster, swRsvp, swEventUpdates, swAnnouncements,
                                 swFriends, swAdminDecs, swCapacity, swRsvpReceived };
        for (SwitchMaterial s : all) s.setEnabled(false);

        mPrefService.getPrefs(uid, prefs -> {
            applyPref(swMaster,        prefs, "push");
            applyPref(swRsvp,          prefs, "rsvp");
            applyPref(swEventUpdates,  prefs, "eventUpdates");
            applyPref(swAnnouncements, prefs, "announcements");
            applyPref(swFriends,       prefs, "friendRequests");
            applyPref(swAdminDecs,     prefs, "adminDecisions");
            applyPref(swCapacity,      prefs, "capacityFull");
            applyPref(swRsvpReceived,  prefs, "rsvpReceived");

            wireToggle(swMaster,        uid, "push");
            wireToggle(swRsvp,          uid, "rsvp");
            wireToggle(swEventUpdates,  uid, "eventUpdates");
            wireToggle(swAnnouncements, uid, "announcements");
            wireToggle(swFriends,       uid, "friendRequests");
            wireToggle(swAdminDecs,     uid, "adminDecisions");
            wireToggle(swCapacity,      uid, "capacityFull");
            wireToggle(swRsvpReceived,  uid, "rsvpReceived");

            for (SwitchMaterial s : all) s.setEnabled(true);
        }, err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
    }

    private void applyPref(SwitchMaterial sw, Map<String, Boolean> prefs, String key) {
        Boolean v = prefs.get(key);
        sw.setChecked(v == null || v);
    }

    private void wireToggle(SwitchMaterial sw, String uid, String key) {
        sw.setOnCheckedChangeListener((btn, checked) -> {
            mPrefService.updatePref(uid, key, checked,
                    () -> {},
                    err -> {
                        // Roll back the visual state on failure.
                        sw.setOnCheckedChangeListener(null);
                        sw.setChecked(!checked);
                        wireToggle(sw, uid, key);
                        Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupOrganizerRequestRow() {
        View     row      = findViewById(R.id.rowRequestOrganizer);
        TextView subtitle = findViewById(R.id.tvRequestOrganizerSubtitle);
        User     user     = RoleUtil.getCurrentUser();

        // Hide entirely for non-students
        if (user == null || !User.ROLE_STUDENT.equals(user.getRole())) {
            row.setVisibility(View.GONE);
            return;
        }

        // Check if a pending request already exists
        mAuthService.fetchOrganizerRequestStatus(
                user.getUid(),
                isPending -> {
                    if (isPending) {
                        disableRequestRow(row, subtitle);
                    } else {
                        enableRequestRow(row, subtitle, user);
                    }
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
    }

    private void enableRequestRow(View row, TextView subtitle, User user) {
        row.setAlpha(1f);
        row.setClickable(true);
        subtitle.setVisibility(View.GONE);
        row.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Request Organizer Role")
                .setMessage("Send a request to the admin to become an organizer?")
                .setPositiveButton("Send Request", (d, w) -> {
                    row.setClickable(false);
                    mAuthService.submitOrganizerRequest(
                            user.getUid(),
                            user.getName(),
                            user.getEmail(),
                            user.getPhotoUrl(),
                            () -> {
                                disableRequestRow(row, subtitle);
                                Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
                            },
                            err -> {
                                row.setClickable(true);
                                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void disableRequestRow(View row, TextView subtitle) {
        row.setAlpha(0.5f);
        row.setClickable(false);
        subtitle.setVisibility(View.VISIBLE);
        subtitle.setText("Request pending");
    }
}
