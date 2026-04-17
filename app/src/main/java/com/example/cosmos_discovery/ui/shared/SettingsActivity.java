package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cosmos_discovery.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvResetPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        int[] stubs = {
            R.id.rowReminders,
            R.id.rowRequestOrganizer,
            R.id.rowHelpCenter,
            R.id.rowContactUs,
            R.id.rowTerms
        };
        for (int id : stubs) {
            findViewById(id).setOnClickListener(v ->
                    Toast.makeText(this, "Will be implemented soon", Toast.LENGTH_SHORT).show());
        }
    }
}
