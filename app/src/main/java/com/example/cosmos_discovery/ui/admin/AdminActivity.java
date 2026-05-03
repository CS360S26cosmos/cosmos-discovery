package com.example.cosmos_discovery.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.ui.shared.SettingsActivity;
import com.example.cosmos_discovery.ui.notifications.NotificationsActivity;
import com.example.cosmos_discovery.ui.student.ViewProfile;
import com.example.cosmos_discovery.util.RoleUtil;

public class AdminActivity extends AppCompatActivity {

    public static final int TAB_DASHBOARD = 0;
    public static final int TAB_EVENTS    = 1;
    public static final int TAB_USERS     = 2;
    public static final int TAB_CATEGORY  = 3;
    private static final String KEY_TAB   = "current_tab";

    private int mCurrentTab = -1;

    private TextView mTextTitle;

    private LinearLayout mNavHome;
    private LinearLayout mNavEvents;
    private LinearLayout mNavUsers;
    private LinearLayout mNavCategory;
    private ImageView    mIconHome;
    private ImageView    mIconEvents;
    private ImageView    mIconUsers;
    private ImageView    mIconCategory;

    private View mSidebarView;

    private final AuthService mAuthService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        com.example.cosmos_discovery.util.NotificationPermission.requestIfNeeded(this);

        bindViews();
        setupTopBar();
        setupNavBar();
        setupSidebar();

        int tab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_TAB, TAB_DASHBOARD)
                : TAB_DASHBOARD;
        selectTab(tab);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, mCurrentTab);
    }

    private void bindViews() {
        mTextTitle    = findViewById(R.id.textTitle);

        mNavHome      = findViewById(R.id.navAdminHome);
        mNavEvents    = findViewById(R.id.navAdminEvents);
        mNavUsers     = findViewById(R.id.navAdminUsers);
        mNavCategory  = findViewById(R.id.navAdminCategory);
        mIconHome     = findViewById(R.id.iconAdminHome);
        mIconEvents   = findViewById(R.id.iconAdminEvents);
        mIconUsers    = findViewById(R.id.iconAdminUsers);
        mIconCategory = findViewById(R.id.iconAdminCategory);

        mSidebarView  = findViewById(R.id.sidebarView);
    }

    private void setupTopBar() {
        ImageView iconMenu = findViewById(R.id.iconMenu);
        iconMenu.setOnClickListener(v -> showSidebar());

        // Bell icon (left) opens the notifications screen
        findViewById(R.id.iconProfile).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void setupNavBar() {
        mNavHome.setOnClickListener(v     -> selectTab(TAB_DASHBOARD));
        mNavEvents.setOnClickListener(v   -> selectTab(TAB_EVENTS));
        mNavUsers.setOnClickListener(v    -> selectTab(TAB_USERS));
        mNavCategory.setOnClickListener(v -> selectTab(TAB_CATEGORY));
    }

    private void selectTab(int tab) {
        if (mCurrentTab == tab) return;
        mCurrentTab = tab;

        Fragment fragment;
        String   title;

        switch (tab) {
            case TAB_EVENTS:
                fragment = new AdminEventQueueFragment();
                title    = "Event Queue";
                break;
            case TAB_USERS:
                fragment = new AdminUserManagementFragment();
                title    = "User Management";
                break;
            case TAB_CATEGORY:
                fragment = new AdminCategoryFragment();
                title    = "Category Manager";
                break;
            default:
                fragment = new AdminDashboardFragment();
                title    = "Dashboard";
                break;
        }

        mTextTitle.setText(title);
        updateNavIcons(tab);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
    }

    private void updateNavIcons(int tab) {
        mIconHome.setImageResource(
                tab == TAB_DASHBOARD ? R.drawable.ic_home_selected : R.drawable.ic_home_outline);
        mIconEvents.setImageResource(
                tab == TAB_EVENTS ? R.drawable.ic_bookmark_selected : R.drawable.ic_bookmark_outline);
        mIconUsers.setImageResource(
                tab == TAB_USERS ? R.drawable.ic_heart_selected : R.drawable.ic_heart_outline);
        mIconCategory.setImageResource(
                tab == TAB_CATEGORY ? R.drawable.ic_category_selected : R.drawable.ic_category_outlined);

        mIconHome.setBackground(null);
        mIconEvents.setBackground(null);
        mIconUsers.setBackground(null);
        mIconCategory.setBackground(null);
    }

    private void setupSidebar() {
        User user = RoleUtil.getCurrentUser();
        if (user != null) {
            TextView name  = mSidebarView.findViewById(R.id.sidebarUserName);
            TextView email = mSidebarView.findViewById(R.id.sidebarUserEmail);
            name.setText(user.getName());
            email.setText(user.getEmail());
            loadSidebarPhoto(user);
        }

        // Admin has no organizer section and no friend requests
        View organizerSection = mSidebarView.findViewById(R.id.organizerSection);
        if (organizerSection != null) organizerSection.setVisibility(View.GONE);
        View friendRequestsRow = mSidebarView.findViewById(R.id.friendRequestsRow);
        if (friendRequestsRow != null) friendRequestsRow.setVisibility(View.GONE);

        mSidebarView.findViewById(R.id.sidebarOverlay).setOnClickListener(v -> hideSidebar());
        mSidebarView.findViewById(R.id.btnCloseSidebar).setOnClickListener(v -> hideSidebar());

        mSidebarView.findViewById(R.id.logoutRow).setOnClickListener(v -> {
            mAuthService.signOut();
            RoleUtil.clear();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        mSidebarView.findViewById(R.id.profileRow).setOnClickListener(v -> {
            hideSidebar();
            startActivity(new Intent(this, ViewProfile.class));
        });

        mSidebarView.findViewById(R.id.settingsRow).setOnClickListener(v -> {
            hideSidebar();
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    private void loadSidebarPhoto(User user) {
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            ImageView icon = mSidebarView.findViewById(R.id.sidebarProfileIcon);
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_sidebar_main_profileimage)
                    .centerCrop()
                    .into(icon);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        User user = RoleUtil.getCurrentUser();
        if (user != null && mSidebarView != null) {
            ((TextView) mSidebarView.findViewById(R.id.sidebarUserName)).setText(user.getName());
            loadSidebarPhoto(user);
        }
    }

    private void showSidebar() {
        View overlay = mSidebarView.findViewById(R.id.sidebarOverlay);
        View panel   = mSidebarView.findViewById(R.id.sidebarPanel);
        float panelWidth = getResources().getDimensionPixelSize(R.dimen.sidebar_width);

        panel.setTranslationX(panelWidth);
        overlay.setAlpha(0f);
        mSidebarView.setVisibility(View.VISIBLE);

        panel.animate().translationX(0f).setDuration(300)
                .setInterpolator(new DecelerateInterpolator()).start();
        overlay.animate().alpha(1f).setDuration(300).start();
    }

    private void hideSidebar() {
        View overlay = mSidebarView.findViewById(R.id.sidebarOverlay);
        View panel   = mSidebarView.findViewById(R.id.sidebarPanel);
        float panelWidth = getResources().getDimensionPixelSize(R.dimen.sidebar_width);

        panel.animate().translationX(panelWidth).setDuration(250)
                .setInterpolator(new AccelerateInterpolator()).start();
        overlay.animate().alpha(0f).setDuration(250)
                .withEndAction(() -> mSidebarView.setVisibility(View.GONE)).start();
    }

    @Override
    public void onBackPressed() {
        if (mSidebarView.getVisibility() == View.VISIBLE) {
            hideSidebar();
        } else {
            super.onBackPressed();
        }
    }
}
