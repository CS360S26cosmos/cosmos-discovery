package com.example.cosmos_discovery.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;

/**
 * Forgot password screen. Accepts an email address and sends a Firebase password
 * reset link via {@link com.example.cosmos_discovery.database.AuthService#sendPasswordReset}.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private EditText mEmailField;
    private Button mSendButton;
    private TextView mBackToSignIn;
    private AuthService mAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuthService   = new AuthService();
        mEmailField = findViewById(R.id.editTextEmail);
        mSendButton = findViewById(R.id.buttonSend);
        mBackToSignIn = findViewById(R.id.textViewBackToSignIn);

        String prefill = getIntent().getStringExtra(EXTRA_EMAIL);
        if (prefill != null && !prefill.isEmpty()) {
            mEmailField.setText(prefill);
        }

        mSendButton.setOnClickListener(v -> {
            String email = mEmailField.getText().toString().trim();
            if (email.isEmpty()) {
                mEmailField.setError("Enter your email."); return;
            }
            mAuthService.sendPasswordReset(email,
                    () -> {
                        Toast.makeText(this,
                                "Reset link sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        finish();
                    },
                    err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            );
        });

        mBackToSignIn.setOnClickListener(v -> finish());
    }
}
