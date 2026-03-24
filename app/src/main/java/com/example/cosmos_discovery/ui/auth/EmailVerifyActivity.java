package com.example.cosmos_discovery.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.ui.shared.MainActivity;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerifyActivity extends AppCompatActivity {

    private TextView mResendLink;
    private AuthService mAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verify);

        mAuthService  = new AuthService();
        mResendLink = findViewById(R.id.textViewResend);

        mResendLink.setOnClickListener(v ->
                mAuthService.resendVerificationEmail(
                        () -> Toast.makeText(this,
                                "Verification email resent.", Toast.LENGTH_SHORT).show(),
                        err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                )
        );
    }

    // When user comes back to the app after clicking the email link,
    // check if they are now verified
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuthService.getCurrentFirebaseUser();
        if (user != null) {
            user.reload().addOnSuccessListener(unused -> {
                if (user.isEmailVerified()) {
                    // Now verified — fetch Firestore doc and proceed
                    mAuthService.fetchUserDocument(user.getUid(),
                            appUser -> {
                                RoleUtil.setCurrentUser(appUser);
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            },
                            err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }
}
