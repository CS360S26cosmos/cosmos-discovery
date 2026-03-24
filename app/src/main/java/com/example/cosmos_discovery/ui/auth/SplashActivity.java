package com.example.cosmos_discovery.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.ui.shared.MainActivity;
import com.example.cosmos_discovery.database.AuthService;

public class SplashActivity extends AppCompatActivity {

    private AuthService mAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No layout needed — just routing logic
        mAuthService = new AuthService();

        if (!mAuthService.isSignedIn()) {
            // Not logged in → go to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Signed in → fetch user doc to get role, then route
        String uid = mAuthService.getCurrentFirebaseUser().getUid();
        mAuthService.fetchUserDocument(uid,
                user -> {
                    // Save role globally for easy access
                    RoleUtil.setCurrentUser(user);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                },
                error -> {
                    // Firestore error on startup → send to login
                    mAuthService.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }
        );
    }
}