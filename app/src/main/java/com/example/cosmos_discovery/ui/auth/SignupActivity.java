package com.example.cosmos_discovery.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;

public class SignupActivity extends AppCompatActivity {

    private EditText mNameField, mEmailField, mPasswordField;
    private Button mSignUpButton;
    private TextView mSignInLink;
    private ProgressBar mProgressBar;
    private AuthService mAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuthService      = new AuthService();
        mNameField     = findViewById(R.id.editTextFullName);
        mEmailField    = findViewById(R.id.editTextCampusEmail);
        mPasswordField = findViewById(R.id.editTextPassword);
        mSignUpButton  = findViewById(R.id.buttonSignUp);
        mSignInLink    = findViewById(R.id.textViewSignIn);
        mProgressBar   = findViewById(R.id.progressBar);

        mSignUpButton.setOnClickListener(v -> attemptSignUp());
        mSignInLink.setOnClickListener(v -> finish()); // go back to Login
    }

    private void attemptSignUp() {
        String name     = mNameField.getText().toString().trim();
        String email    = mEmailField.getText().toString().trim();
        String password = mPasswordField.getText().toString();

        if (name.isEmpty()) {
            mNameField.setError("Full name is required.");  return;
        }
        if (!mAuthService.isUniversityEmail(email)) {
            mEmailField.setError("Use your university email (@lums.edu.pk)."); return;
        }
        if (!mAuthService.isValidPassword(password)) {
            mPasswordField.setError("Password must be at least 6 characters."); return;
        }

        setLoading(true);

        mAuthService.signUp(email, password, name,
                user -> {
                    setLoading(false);
                    // Account created → go to verify screen
                    startActivity(new Intent(this, EmailVerifyActivity.class));
                    finish();
                },
                error -> {
                    setLoading(false);
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
        );
    }

    private void setLoading(boolean loading) {
        mProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        mSignUpButton.setEnabled(!loading);
    }
}
