package com.example.cosmos_discovery.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User currentUser = RoleUtil.getCurrentUser();
        if (currentUser == null) {
            // Safety net — should never happen if SplashActivity is correct
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        if (currentUser.getRole().equals(User.ROLE_ADMIN)) {
            // Load admin navigation (4 tabs: Home/Events/Users/Category)
            bottomNav.inflateMenu(R.menu.menu_admin_nav);
            loadAdminNavigation(bottomNav);
        } else {
            // Load student/organizer navigation (3 tabs: Home/My Events/Friends)
            bottomNav.inflateMenu(R.menu.menu_student_nav);
            loadStudentNavigation(bottomNav);
        }
    }

    private void loadStudentNavigation(BottomNavigationView nav) {
        // Default fragment = DiscoverFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new DiscoverFragment())
                .commit();

        nav.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();
            if (id == R.id.nav_home)      selected = new DiscoverFragment();
            else if (id == R.id.nav_my_events) selected = new MyEventsFragment();
            else                           selected = new PlaceholderFragment(); // Friends
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selected).commit();
            return true;
        });
    }

    private void loadAdminNavigation(BottomNavigationView nav) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new AdminDashboardFragment())
                .commit();
        // ... similar pattern for admin tabs
    }
}