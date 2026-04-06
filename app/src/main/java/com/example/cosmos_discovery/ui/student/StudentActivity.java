package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.FilterState;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private boolean      mInUserSearchMode = false;
    private int          mTabBeforeSearch  = TAB_DISCOVER;
    private SearchFragment     mSearchFragment;
    private UserSearchFragment mUserSearchFragment;

    private final TextWatcher mSearchTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mSearchFragment != null) mSearchFragment.updateQuery(s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    private final TextWatcher mUserSearchTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mUserSearchFragment != null) mUserSearchFragment.updateQuery(s.toString());
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

    // Filter overlay
    private View         mFilterOverlay;
    private List<String> mCachedCategories;   // null = not yet fetched
    private boolean      mFilterChipsWired = false;

    // Pending chip selections while the filter overlay is open
    private final Set<String>                mPendingCategories   = new HashSet<>();
    private final Map<String, TextView>      mActiveCategoryChips = new HashMap<>();
    private FilterState.DateFilter           mPendingDate;
    private FilterState.AccessFilter         mPendingAccess;

    // Active chip reference for single-select groups (needed to deselect)
    private TextView mActiveDateChip;
    private TextView mActiveAccessChip;

    private final AuthService  mAuthService  = new AuthService();
    private final EventService mEventService = new EventService();

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

        mSidebarView   = findViewById(R.id.sidebarView);
        mFilterOverlay = findViewById(R.id.filterOverlay);
    }

    /** Wires the menu icon, search bar pill, and filter button click listeners. */
    private void setupTopBar() {
        // Menu icon (right) opens the sidebar
        ImageView iconMenu = findViewById(R.id.iconMenu);
        iconMenu.setOnClickListener(v -> showSidebar());

        // Profile icon (left) opens the current user's profile
        findViewById(R.id.iconProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ViewProfile.class)));

        // Search bar tap → enter event search or user search based on active tab
        mSearchBarInclude.findViewById(R.id.searchClickableArea)
                .setOnClickListener(v -> {
                    if (mCurrentTab == TAB_FRIENDS) enterUserSearchMode();
                    else enterSearchMode();
                });

        // Filter button tap → enter search mode (if needed) then show filter overlay
        mSearchBarInclude.findViewById(R.id.buttonFilter)
                .setOnClickListener(v -> onFilterButtonClick());

        // Same filter button, but shown in the active search bar (pill is GONE in search mode)
        mSearchActiveBar.findViewById(R.id.filterButtonActive)
                .setOnClickListener(v -> showFilterOverlay());
    }

    /** Wires bottom nav tab click listeners. */
    private void setupNavBar() {
        mNavHome.setOnClickListener(v     -> navigateToTab(TAB_DISCOVER));
        mNavMyEvents.setOnClickListener(v -> navigateToTab(TAB_MY_EVENTS));
        mNavFriends.setOnClickListener(v  -> navigateToTab(TAB_FRIENDS));
    }

    /** Exits search mode if active, then selects the given tab. */
    private void navigateToTab(int tab) {
        if (mInSearchMode) exitSearchMode();
        selectTab(tab);
    }

    /** Populates the sidebar with the current user's name and email, and wires its action listeners. */
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

        mSidebarView.findViewById(R.id.profileRow)
                .setOnClickListener(v -> {
                    hideSidebar();

                    Intent intent = new Intent(this, ViewProfile.class);
                    startActivity(intent);
                });
    }

    // ── Navigation ───────────────────────────────────────────────────────

    /**
     * Replaces the fragment container with the fragment for the given tab and
     * updates the top-bar title and nav icons. No-op if the tab is already active.
     *
     * @param tab One of {@link #TAB_DISCOVER}, {@link #TAB_MY_EVENTS}, {@link #TAB_FRIENDS}.
     */
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

        // Search bar is visible on Discover and Friends tabs; hint text and filter button differ
        boolean showSearch = (tab == TAB_DISCOVER || tab == TAB_FRIENDS);
        mSearchBarInclude.setVisibility(showSearch ? View.VISIBLE : View.GONE);
        if (showSearch) {
            ((TextView) mSearchBarInclude.findViewById(R.id.textSearchPlaceholder))
                    .setText(tab == TAB_FRIENDS ? "Search Friends" : "Search Events");
            mSearchBarInclude.findViewById(R.id.buttonFilter)
                    .setVisibility(tab == TAB_FRIENDS ? View.GONE : View.VISIBLE);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.studentFragmentContainer, fragment)
                .commit();
    }

    /** Swaps nav icons between filled (selected) and outline (unselected) drawables. */
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
        enterSearchMode(true);
    }

    /**
     * Switches the top bar to active-search mode: hides the pill, shows the EditText,
     * replaces the current fragment with {@link SearchFragment}, and optionally
     * opens the keyboard.
     *
     * @param showKeyboard {@code true} to immediately open the soft keyboard.
     */
    private void enterSearchMode(boolean showKeyboard) {
        mInSearchMode    = true;
        mTabBeforeSearch = mCurrentTab;

        mSearchBarInclude.setVisibility(View.GONE);
        mSearchActiveBar.setVisibility(View.VISIBLE);

        mSearchFragment = new SearchFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.studentFragmentContainer, mSearchFragment)
                .commit();

        mEtSearchInput.addTextChangedListener(mSearchTextWatcher);

        if (showKeyboard) {
            mEtSearchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(mEtSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Opens the search bar in user-search mode (Friends tab).
     * Loads {@link UserSearchFragment} and wires the user-search text watcher.
     */
    private void enterUserSearchMode() {
        mInSearchMode     = true;
        mInUserSearchMode = true;
        mTabBeforeSearch  = mCurrentTab;

        mSearchBarInclude.setVisibility(View.GONE);
        mSearchActiveBar.setVisibility(View.VISIBLE);
        mEtSearchInput.setHint("Search Friends");
        mSearchActiveBar.findViewById(R.id.filterButtonActive).setVisibility(View.GONE);

        mUserSearchFragment = new UserSearchFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.studentFragmentContainer, mUserSearchFragment)
                .commit();

        mEtSearchInput.addTextChangedListener(mUserSearchTextWatcher);
        mEtSearchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEtSearchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Restores the top bar from search mode: closes the filter overlay (if open), clears
     * the search text, hides the keyboard, and restores the tab that was active before
     * search was entered.
     */
    private void exitSearchMode() {
        mInSearchMode = false;

        if (mFilterOverlay.getVisibility() == View.VISIBLE) hideFilterOverlay();
        resetPendingFilters();

        if (mInUserSearchMode) {
            mEtSearchInput.removeTextChangedListener(mUserSearchTextWatcher);
            mEtSearchInput.setHint("Search Events");
            mSearchActiveBar.findViewById(R.id.filterButtonActive).setVisibility(View.VISIBLE);
            mInUserSearchMode   = false;
            mUserSearchFragment = null;
        } else {
            mEtSearchInput.removeTextChangedListener(mSearchTextWatcher);
            mSearchFragment = null;
        }

        mEtSearchInput.setText("");

        mSearchActiveBar.setVisibility(View.GONE);
        mSearchBarInclude.setVisibility(View.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEtSearchInput.getWindowToken(), 0);

        mCurrentTab = -1; // force fragment reload
        selectTab(mTabBeforeSearch);
    }

    // ── Filter overlay helpers ────────────────────────────────────────────

    private void onFilterButtonClick() {
        if (!mInSearchMode) enterSearchMode(false); // no keyboard — user is filtering, not typing
        showFilterOverlay();
    }

    private void showFilterOverlay() {
        mFilterOverlay.setVisibility(View.VISIBLE);
        if (!mFilterChipsWired) wireFilterChips();
        if (mCachedCategories == null) {
            mEventService.fetchCategories(
                    names -> { mCachedCategories = names; inflateCategoryChips(names); },
                    err   -> {
                        mFilterOverlay.findViewById(R.id.tvCategories).setVisibility(View.GONE);
                        mFilterOverlay.findViewById(R.id.categoryChipsContainer).setVisibility(View.GONE);
                    });
        } else {
            inflateCategoryChips(mCachedCategories);
        }
    }

    private void hideFilterOverlay() {
        mFilterOverlay.setVisibility(View.GONE);
    }

    /** Wires click listeners for all static chips and action buttons. Called once. */
    private void wireFilterChips() {
        mFilterChipsWired = true;

        TextView chipToday       = mFilterOverlay.findViewById(R.id.chipToday);
        TextView chipTomorrow    = mFilterOverlay.findViewById(R.id.chipTomorrow);
        TextView chipThisWeek    = mFilterOverlay.findViewById(R.id.chipThisWeek);
        TextView chipThisWeekend = mFilterOverlay.findViewById(R.id.chipThisWeekend);
        TextView chipThisMonth   = mFilterOverlay.findViewById(R.id.chipThisMonth);
        TextView chipLums        = mFilterOverlay.findViewById(R.id.chipLums);
        TextView chipOutsiders   = mFilterOverlay.findViewById(R.id.chipOutsiders);

        chipToday.setOnClickListener(v       -> onDateChipClick(chipToday,       FilterState.DateFilter.TODAY));
        chipTomorrow.setOnClickListener(v    -> onDateChipClick(chipTomorrow,    FilterState.DateFilter.TOMORROW));
        chipThisWeek.setOnClickListener(v    -> onDateChipClick(chipThisWeek,    FilterState.DateFilter.THIS_WEEK));
        chipThisWeekend.setOnClickListener(v -> onDateChipClick(chipThisWeekend, FilterState.DateFilter.THIS_WEEKEND));
        chipThisMonth.setOnClickListener(v   -> onDateChipClick(chipThisMonth,   FilterState.DateFilter.THIS_MONTH));

        chipLums.setOnClickListener(v      -> onAccessChipClick(chipLums,      FilterState.AccessFilter.LUMS_ONLY));
        chipOutsiders.setOnClickListener(v -> onAccessChipClick(chipOutsiders, FilterState.AccessFilter.OPEN));

        // Backdrop tap closes without applying
        mFilterOverlay.setOnClickListener(v -> hideFilterOverlay());
        mFilterOverlay.findViewById(R.id.btnClose)
                .setOnClickListener(v -> hideFilterOverlay());
        mFilterOverlay.findViewById(R.id.btnClearFilters)
                .setOnClickListener(v -> onClearFiltersClick());
        mFilterOverlay.findViewById(R.id.btnViewResults)
                .setOnClickListener(v -> onViewResultsClick());
    }

    /** Inflates category chips from Firestore into the FlexboxLayout. Re-syncs selection state. */
    private void inflateCategoryChips(List<String> categories) {
        FlexboxLayout container = mFilterOverlay.findViewById(R.id.categoryChipsContainer);
        container.removeAllViews();

        float density    = getResources().getDisplayMetrics().density;
        int   paddingH   = (int)(14 * density);
        int   marginEnd  = (int)(10 * density);
        int   marginBot  = (int)(8  * density);
        int   chipHeight = (int)(23 * density);

        for (String name : categories) {
            TextView chip = new TextView(this);
            chip.setText(name);
            chip.setTextSize(12f);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(paddingH, 0, paddingH, 0);

            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, chipHeight);
            lp.setMargins(0, 0, marginEnd, marginBot);
            chip.setLayoutParams(lp);

            if (mPendingCategories.contains(name)) {
                selectChip(chip);
                mActiveCategoryChips.put(name, chip);
            } else {
                deselectChip(chip);
            }

            chip.setOnClickListener(v -> onCategoryChipClick(chip, name));
            container.addView(chip);
        }
    }

    // ── Chip toggle helpers ───────────────────────────────────────────────

    private void selectChip(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_selected);
        chip.setTextColor(getColor(R.color.color_text_on_primary));
    }

    private void deselectChip(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_unselected);
        chip.setTextColor(getColor(R.color.color_text_secondary));
    }

    private void onDateChipClick(TextView chip, FilterState.DateFilter value) {
        if (mActiveDateChip != null) deselectChip(mActiveDateChip);
        if (mPendingDate == value) { // tap same chip → toggle off
            mPendingDate = null; mActiveDateChip = null; return;
        }
        selectChip(chip);
        mPendingDate = value;
        mActiveDateChip = chip;
    }

    private void onAccessChipClick(TextView chip, FilterState.AccessFilter value) {
        if (mActiveAccessChip != null) deselectChip(mActiveAccessChip);
        if (mPendingAccess == value) {
            mPendingAccess = null; mActiveAccessChip = null; return;
        }
        selectChip(chip);
        mPendingAccess = value;
        mActiveAccessChip = chip;
    }

    private void onCategoryChipClick(TextView chip, String name) {
        if (mPendingCategories.contains(name)) {
            // Tap same chip again → deselect it
            deselectChip(chip);
            mPendingCategories.remove(name);
            mActiveCategoryChips.remove(name);
        } else {
            // New chip → add to selection
            selectChip(chip);
            mPendingCategories.add(name);
            mActiveCategoryChips.put(name, chip);
        }
    }

    // ── Filter action buttons ─────────────────────────────────────────────

    private void onViewResultsClick() {
        FilterState fs = new FilterState();
        fs.setSelectedCategories(new ArrayList<>(mPendingCategories));
        fs.setSelectedDate(mPendingDate);
        fs.setSelectedAccess(mPendingAccess);
        if (mSearchFragment != null) mSearchFragment.updateFilters(fs);
        hideFilterOverlay();
    }

    private void onClearFiltersClick() {
        if (mActiveDateChip   != null) { deselectChip(mActiveDateChip);   mActiveDateChip   = null; }
        if (mActiveAccessChip != null) { deselectChip(mActiveAccessChip); mActiveAccessChip = null; }
        for (TextView chip : mActiveCategoryChips.values()) deselectChip(chip);
        mActiveCategoryChips.clear();
        mPendingCategories.clear();
        mPendingDate = null; mPendingAccess = null;
        if (mSearchFragment != null) mSearchFragment.updateFilters(new FilterState());
        hideFilterOverlay();
    }

    /** Resets all pending filter state and deselects chips. Called on exitSearchMode(). */
    private void resetPendingFilters() {
        if (mActiveDateChip   != null) { deselectChip(mActiveDateChip);   mActiveDateChip   = null; }
        if (mActiveAccessChip != null) { deselectChip(mActiveAccessChip); mActiveAccessChip = null; }
        for (TextView chip : mActiveCategoryChips.values()) deselectChip(chip);
        mActiveCategoryChips.clear();
        mPendingCategories.clear();
        mPendingDate = null; mPendingAccess = null;
    }

    // ── Sidebar helpers ──────────────────────────────────────────────────

    /** Slides the sidebar panel in from the right with a fade-in overlay. */
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

    /** Slides the sidebar panel out to the right with a fade-out overlay. */
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

    /** Exit search, close filter overlay, or close sidebar on back press before delegating to super. */
    @Override
    public void onBackPressed() {
        if (mFilterOverlay != null && mFilterOverlay.getVisibility() == View.VISIBLE) {
            hideFilterOverlay();
        } else if (mInSearchMode) {
            exitSearchMode();
        } else if (mSidebarView.getVisibility() == View.VISIBLE) {
            hideSidebar();
        } else {
            super.onBackPressed();
        }
    }

}
