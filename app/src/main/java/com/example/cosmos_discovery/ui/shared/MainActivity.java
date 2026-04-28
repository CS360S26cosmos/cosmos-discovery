package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.ui.admin.AdminActivity;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.util.RoleUtil;

/**
 * Post-login router. Delegates to the role-specific shell activity.
 * Shows a non-dismissible promotion popup if the user's request was approved.
 */
public class MainActivity extends AppCompatActivity {

    private final AuthService mAuthService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User user = RoleUtil.getCurrentUser();

        if (user != null && user.isPromotionApproved()) {
            showPromotionPopup(user);
        } else {
            routeUser(user);
        }
    }

    private void showPromotionPopup(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Request Approved!")
                .setMessage("Your request to become an organizer has been approved. "
                        + "Sign out now to access the Organizer panel.")
                .setCancelable(false)
                .setPositiveButton("Sign Out Now", (d, w) -> {
                    mAuthService.clearPromotionFlag(
                            user.getUid(),
                            () -> signOutAndGoToLogin(),
                            err -> signOutAndGoToLogin());
                })
                .setNegativeButton("Later", (d, w) -> routeUser(user))
                .show();
    }

    private void signOutAndGoToLogin() {
        mAuthService.signOut();
        RoleUtil.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void routeUser(User user) {
        Class<?> destination;
        if (RoleUtil.isAdmin()) {
            destination = AdminActivity.class;
        } else {
            destination = StudentActivity.class;
        }
        startActivity(new Intent(this, destination));
        finish();
    }
}
