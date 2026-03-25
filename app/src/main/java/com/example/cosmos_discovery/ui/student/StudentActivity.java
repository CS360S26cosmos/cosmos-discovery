package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.util.RoleUtil;

/**
 * Shell activity for the student role.
 *
 * Owns all persistent chrome: purple top bar, bottom nav bar, and side drawer.
 * Swaps content fragments ({@link DiscoverFragment}, {@link MyEventsFragment},
 * {@link FriendsFragment}) inside {@code studentFragmentContainer}.
 */
public class StudentActivity extends AppCompatActivity {

    private static final int TAB_DISCOVER   = 0;
    private static final int TAB_MY_EVENTS  = 1;
    private static final int TAB_FRIENDS    = 2;
    private static final String KEY_TAB     = "current_tab";

    private int mCurrentTab = -1; // -1 forces the first selectTab() call to load

    // Top bar
    private TextView  mTextTitle;

    // Search bar
    private View         mSearchBarInclude;
    private View         mSearchActiveBar;
    private EditText     mEtSearchInput;
    private boolean      mInSearchMode     = false;
    private int          mTabBeforeSearch  = TAB_DISCOVER;
    private SearchFragment mSearchFragment;

    private final TextWatcher mSearchTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mSearchFragment != null) mSearchFragment.updateQuery(s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    // Nav bar tabs + icons
    private LinearLayout mNavHome;
    private LinearLayout mNavMyEvents;
    private LinearLayout mNavFriends;
    private ImageView    mIconHome;
    private ImageView    mIconMyEvents;
    private ImageView    mIconFriends;

    // Sidebar root view
    private View mSidebarView;

    private final AuthService mAuthService = new AuthService();

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        bindViews();
        setupTopBar();
        setupNavBar();
        setupSidebar();

        int tab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_TAB, TAB_DISCOVER)
                : TAB_DISCOVER;
        selectTab(tab);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, mCurrentTab);
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    private void bindViews() {
        mTextTitle    = findViewById(R.id.textTitle);

        mSearchBarInclude = findViewById(R.id.searchBarInclude);
        mSearchActiveBar  = findViewById(R.id.searchActiveBar);
        mEtSearchInput    = findViewById(R.id.etSearchInput);

        mNavHome      = findViewById(R.id.navHome);
        mNavMyEvents  = findViewById(R.id.navMyEvents);
        mNavFriends   = findViewById(R.id.navFriends);
        mIconHome     = findViewById(R.id.iconHome);
        mIconMyEvents = findViewById(R.id.iconMyEvents);
        mIconFriends  = findViewById(R.id.iconFriends);

        mSidebarView  = findViewById(R.id.sidebarView);
    }

    private void setupTopBar() {
        // Menu icon (right) opens the sidebar
        ImageView iconMenu = findViewById(R.id.iconMenu);
        iconMenu.setOnClickListener(v -> showSidebar());
        // Profile icon (left) — placeholder, no action yet

        // Search bar tap → enter search mode
        mSearchBarInclude.findViewById(R.id.searchClickableArea)
                .setOnClickListener(v -> enterSearchMode());


    }

    private void setupNavBar() {
        mNavHome.setOnClickListener(v     -> selectTab(TAB_DISCOVER));
        mNavMyEvents.setOnClickListener(v -> selectTab(TAB_MY_EVENTS));
        mNavFriends.setOnClickListener(v  -> selectTab(TAB_FRIENDS));
    }

    private void setupSidebar() {
        // Populate user info from the in-memory current user
        User user = RoleUtil.getCurrentUser();
        if (user != null) {
            TextView name  = mSidebarView.findViewById(R.id.sidebarUserName);
            TextView email = mSidebarView.findViewById(R.id.sidebarUserEmail);
            name.setText(user.getName());
            email.setText(user.getEmail());
        }

        // Dim overlay and X button both close the sidebar
        mSidebarView.findViewById(R.id.sidebarOverlay)
                    .setOnClickListener(v -> hideSidebar());
        mSidebarView.findViewById(R.id.btnCloseSidebar)
                    .setOnClickListener(v -> hideSidebar());

        // Logout row — sign out and return to Login
        mSidebarView.findViewById(R.id.logoutRow)
                    .setOnClickListener(v -> {
                        mAuthService.signOut();
                        RoleUtil.clear();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
    }

    // ── Navigation ───────────────────────────────────────────────────────

    private void selectTab(int tab) {
        if (mCurrentTab == tab) return;
        mCurrentTab = tab;

        Fragment fragment;
        String   title;

        switch (tab) {
            case TAB_MY_EVENTS:
                fragment = new MyEventsFragment();
                title    = "My Events";
                break;
            case TAB_FRIENDS:
                fragment = new FriendsFragment();
                title    = "Friends";
                break;
            default:
                fragment = new DiscoverFragment();
                title    = "Discover";
                break;
        }

        mTextTitle.setText(title);
        updateNavIcons(tab);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.studentFragmentContainer, fragment)
                .commit();
    }

    private void updateNavIcons(int tab) {
        // Swap filled ↔ outline icon
        mIconHome.setImageResource(
                tab == TAB_DISCOVER  ? R.drawable.ic_home_selected     : R.drawable.ic_home_outline);
        mIconMyEvents.setImageResource(
                tab == TAB_MY_EVENTS ? R.drawable.ic_bookmark_selected : R.drawable.ic_bookmark_outline);
        mIconFriends.setImageResource(
                tab == TAB_FRIENDS   ? R.drawable.ic_heart_selected    : R.drawable.ic_heart_outline);

        // Clear any leftover background — the highlight is baked into the _selected layer-list
        mIconHome.setBackground(null);
        mIconMyEvents.setBackground(null);
        mIconFriends.setBackground(null);
    }

    // ── Search helpers ────────────────────────────────────────────────────

    private void enterSearchMode() {
        mInSearchMode    = true;
        mTabBeforeSearch = mCurrentTab;

        mSearchBarInclude.setVisibility(View.GONE);
        mSearchActiveBar.setVisibility(View.VISIBLE);

        mSearchFragment = new SearchFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.studentFragmentContainer, mSearchFragment)
                .commit();

        mEtSearchInput.requestFocus();
        mEtSearchInput.addTextChangedListener(mSearchTextWatcher);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEtSearchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void exitSearchMode() {
        mInSearchMode = false;

        mEtSearchInput.removeTextChangedListener(mSearchTextWatcher);
        mEtSearchInput.setText("");

        mSearchActiveBar.setVisibility(View.GONE);
        mSearchBarInclude.setVisibility(View.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEtSearchInput.getWindowToken(), 0);

        mSearchFragment = null;
        mCurrentTab = -1; // force fragment reload
        selectTab(mTabBeforeSearch);
    }

    // ── Sidebar helpers ──────────────────────────────────────────────────

    private void showSidebar() {
        View overlay = mSidebarView.findViewById(R.id.sidebarOverlay);
        View panel   = mSidebarView.findViewById(R.id.sidebarPanel);
        float panelWidth = getResources().getDimensionPixelSize(R.dimen.sidebar_width);

        // Panel starts offscreen right, slides in
        panel.setTranslationX(panelWidth);
        // Overlay starts invisible, fades in
        overlay.setAlpha(0f);

        mSidebarView.setVisibility(View.VISIBLE);

        panel.animate()
                .translationX(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        overlay.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void hideSidebar() {
        View overlay = mSidebarView.findViewById(R.id.sidebarOverlay);
        View panel   = mSidebarView.findViewById(R.id.sidebarPanel);
        float panelWidth = getResources().getDimensionPixelSize(R.dimen.sidebar_width);

        panel.animate()
                .translationX(panelWidth)
                .setDuration(250)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        overlay.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> mSidebarView.setVisibility(View.GONE))
                .start();
    }

    /** Exit search or close sidebar on back press before delegating to super. */
    @Override
    public void onBackPressed() {
        if (mInSearchMode) {
            exitSearchMode();
        } else if (mSidebarView.getVisibility() == View.VISIBLE) {
            hideSidebar();
        } else {
            super.onBackPressed();
        }
    }
}
