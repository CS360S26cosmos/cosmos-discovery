package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;

public class ViewProfile extends AppCompatActivity {

    private TextView tvName, tvMajor, tvBio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile); // your XML

        // Bind views
        tvName  = findViewById(R.id.tvName);
        tvMajor = findViewById(R.id.tvMajor);
        tvBio   = findViewById(R.id.tvBio);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get user
        User user = RoleUtil.getCurrentUser();

        // Populate UI
        if (user != null) {
            tvName.setText(user.getName() != null ? user.getName() : "");
            tvMajor.setText(user.getMajor() != null ? user.getMajor() : "");
            tvBio.setText(user.getBio() != null ? user.getBio() : "");
        }

        findViewById(R.id.btnEdtProfile).setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfile.this, EditProfile.class);
            startActivity(intent);
        });
    }
}