package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.ui.student.StudentActivity;

/**
 * Post-login router. Immediately delegates to the role-specific shell activity.
 * Currently only {@link com.example.cosmos_discovery.ui.student.StudentActivity}
 * is fully implemented; organizer and admin shells are placeholders.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Route all roles to StudentActivity for now.
        // Organizer and Admin shells are not yet implemented — they fall back to the
        // student shell so the full chrome (top bar, nav bar, sidebar) is always shown.
        startActivity(new Intent(this, StudentActivity.class));
        finish();
    }
}
