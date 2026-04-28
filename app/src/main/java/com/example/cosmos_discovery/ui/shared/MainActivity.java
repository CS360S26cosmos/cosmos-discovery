package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.ui.admin.AdminActivity;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.RoleUtil;

/**
 * Post-login router. Immediately delegates to the role-specific shell activity.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Class<?> destination = RoleUtil.isAdmin() ? AdminActivity.class : StudentActivity.class;
        startActivity(new Intent(this, destination));
        finish();
    }
}
