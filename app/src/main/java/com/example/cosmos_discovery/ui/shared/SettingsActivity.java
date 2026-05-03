package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;

public class SettingsActivity extends AppCompatActivity {

    private final AuthService mAuthService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvResetPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        // Stubs
        int[] stubs = { R.id.rowReminders, R.id.rowHelpCenter, R.id.rowContactUs, R.id.rowTerms };
        for (int id : stubs) {
            findViewById(id).setOnClickListener(v ->
                    Toast.makeText(this, "Will be implemented soon", Toast.LENGTH_SHORT).show());
        }

        setupOrganizerRequestRow();
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
