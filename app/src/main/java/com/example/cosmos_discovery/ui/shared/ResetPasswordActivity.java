package com.example.cosmos_discovery.ui.shared;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null ? user.getEmail() : "";

        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setText(email);

        AuthService authService = new AuthService();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Button btnSend = findViewById(R.id.buttonSend);
        btnSend.setOnClickListener(v -> {
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "No email found for your account.", Toast.LENGTH_LONG).show();
                return;
            }
            btnSend.setEnabled(false);
            authService.sendPasswordReset(email,
                    () -> {
                        Toast.makeText(this, "Reset link sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        finish();
                    },
                    err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                        btnSend.setEnabled(true);
                    }
            );
        });
    }
}
