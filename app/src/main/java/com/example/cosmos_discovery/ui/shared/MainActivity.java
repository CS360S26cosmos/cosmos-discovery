package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.ui.admin.AdminActivity;
import com.example.cosmos_discovery.ui.organizer.OrganizerActivity;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.RoleUtil;

public class MainActivity extends AppCompatActivity {

    private AuthService mAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuthService = new AuthService();

        if (RoleUtil.getCurrentUser() == null) {
            mAuthService.signOut();
            finish();
            return;
        }

        routeUser();
    }

    private void routeUser() {
        Intent intent;

        if (RoleUtil.isAdmin()) {
            intent = new Intent(this, AdminActivity.class);

        } else if (RoleUtil.isOrganizer()) {
            intent = new Intent(this, OrganizerActivity.class);

        } else {
            intent = new Intent(this, StudentActivity.class);
        }

        startActivity(intent);
        finish();
    }
}