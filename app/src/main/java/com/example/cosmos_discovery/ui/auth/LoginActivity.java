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

import com.example.cosmos_discovery.ui.shared.MainActivity;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.util.RoleUtil;

/**
 * Login screen. Validates the university email domain and delegates sign-in to
 * {@link com.example.cosmos_discovery.database.AuthService}. Routes to
 * {@link com.example.cosmos_discovery.ui.shared.MainActivity} on success,
 * or {@link EmailVerifyActivity} if the email has not yet been verified.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText mEmailField, mPasswordField;
    private Button mSignInButton;
    private TextView mForgotPassword, mSignUpLink;
    private ProgressBar mProgressBar;
    private AuthService mAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuthService     = new AuthService();
        mEmailField   = findViewById(R.id.editTextEmail);
        mPasswordField= findViewById(R.id.editTextPassword);
        mSignInButton = findViewById(R.id.buttonSignIn);
        mForgotPassword = findViewById(R.id.textViewForgotPassword);
        mSignUpLink   = findViewById(R.id.textViewSignUp);
        mProgressBar  = findViewById(R.id.progressBar);

        mSignInButton.setOnClickListener(v -> attemptLogin());

        mForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        mSignUpLink.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    /**
     * Validates the input fields client-side then calls
     * {@link com.example.cosmos_discovery.database.AuthService#signIn} with the entered credentials.
     */
    private void attemptLogin() {
        String email    = mEmailField.getText().toString().trim();
        String password = mPasswordField.getText().toString();

        // Client-side validation before hitting Firebase
        if (email.isEmpty()) {
            mEmailField.setError("Email is required.");
            return;
        }
        if (!mAuthService.isUniversityEmail(email)) {
            mEmailField.setError("Use your university email (@lums.edu.pk).");
            return;
        }
        if (password.isEmpty()) {
            mPasswordField.setError("Password is required.");
            return;
        }

        setLoading(true);

        mAuthService.signIn(email, password,
                user -> {
                    // Success
                    setLoading(false);
                    RoleUtil.setCurrentUser(user);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                },
                () -> {
                    // Email not verified
                    setLoading(false);
                    startActivity(new Intent(this, EmailVerifyActivity.class));
                },
                error -> {
                    setLoading(false);
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
        );
    }

    /**
     * Shows or hides the progress spinner and disables or enables the sign-in button.
     *
     * @param loading {@code true} to show the spinner and disable the button.
     */
    private void setLoading(boolean loading) {
        mProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        mSignInButton.setEnabled(!loading);
    }
}
