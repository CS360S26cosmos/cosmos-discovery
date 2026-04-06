package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.ui.student.DiscoverFragment;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.RoleUtil;

/**
 * Post-login router. Immediately delegates to the role-specific shell activity.
 * Currently only {@link com.example.cosmos_discovery.ui.student.StudentActivity}
 * is fully implemented; organizer and admin shells are placeholders.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, StudentActivity.class));
        finish();
    }
}
