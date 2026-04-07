package com.example.cosmos_discovery.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.OrganizerEventAdapter;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.ui.student.StudentActivity;
import com.example.cosmos_discovery.ui.student.ViewProfile;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class OrganizerActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerActivity";

    private final AuthService  mAuthService  = new AuthService();
    private final EventService mEventService = new EventService();

    private ListenerRegistration mEventsListener;

    private OrganizerEventAdapter mAdapter;
    private TextView              mTvEmpty;
    private View                  mSidebarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        if (RoleUtil.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        RecyclerView rv = findViewById(R.id.rvPostedEvents);
        mTvEmpty = findViewById(R.id.tvEmptyPostedEvents);

        mAdapter = new OrganizerEventAdapter(this, new ArrayList<>(), this::onEventClick);
        rv.setAdapter(mAdapter);

        findViewById(R.id.btnCreateEventCard).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEventActivity.class));
        });

        setupTopBar();
        setupSidebar();
        wireBottomNav();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mEventsListener != null) {
            mEventsListener.remove();
            mEventsListener = null;
        }
    }

    private void attachListener() {
        if (mEventsListener != null) return;
        String uid = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : null;
        if (uid == null || uid.trim().isEmpty()) return;

        mEventsListener = mEventService.listenOrganizerEvents(
                uid,
                this::onEventsUpdate,
                err -> {
                    if (err.contains("FAILED_PRECONDITION")) {
                        Log.w(TAG, err);
                        Toast.makeText(this, "Events are loading. Please try again shortly.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void onEventsUpdate(List<Event> events) {
        mAdapter.updateData(events);
        mTvEmpty.setVisibility(events == null || events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onEventClick(Event event) {
        if (event == null || event.getId() == null) return;
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    private void setupTopBar() {
        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.textTitle);
        if (title != null) title.setText("Organizer");

        View iconMenu = findViewById(R.id.iconMenu);
        if (iconMenu != null) iconMenu.setOnClickListener(v -> showSidebar());
    }

    private void setupSidebar() {
        mSidebarView = findViewById(R.id.sidebarView);
        if (mSidebarView == null) return;

        if (RoleUtil.getCurrentUser() != null) {
            TextView name  = mSidebarView.findViewById(R.id.sidebarUserName);
            TextView email = mSidebarView.findViewById(R.id.sidebarUserEmail);
            name.setText(RoleUtil.getCurrentUser().getName());
            email.setText(RoleUtil.getCurrentUser().getEmail());
        }

        View organizerSection = mSidebarView.findViewById(R.id.organizerSection);
        if (organizerSection != null) organizerSection.setVisibility(View.VISIBLE);
        View posted = mSidebarView.findViewById(R.id.organizerPostedEventsRow);
        View create = mSidebarView.findViewById(R.id.organizerCreateEventRow);
        if (posted != null) posted.setOnClickListener(v -> hideSidebar());
        if (create != null) create.setOnClickListener(v -> {
            hideSidebar();
            startActivity(new Intent(this, AddEventActivity.class));
        });

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
            startActivity(new Intent(this, com.example.cosmos_discovery.ui.shared.SettingsActivity.class));
        });
    }

    private void wireBottomNav() {
        if (findViewById(R.id.navHome) == null) return;
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentActivity.class);
            intent.putExtra(StudentActivity.EXTRA_START_TAB, StudentActivity.TAB_DISCOVER);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentActivity.class);
            intent.putExtra(StudentActivity.EXTRA_START_TAB, StudentActivity.TAB_MY_EVENTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        findViewById(R.id.navFriends).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentActivity.class);
            intent.putExtra(StudentActivity.EXTRA_START_TAB, StudentActivity.TAB_FRIENDS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    private void showSidebar() {
        if (mSidebarView == null) return;
        View overlay = mSidebarView.findViewById(R.id.sidebarOverlay);
        View panel   = mSidebarView.findViewById(R.id.sidebarPanel);
        float panelWidth = getResources().getDimensionPixelSize(R.dimen.sidebar_width);

        panel.setTranslationX(panelWidth);
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
        if (mSidebarView == null) return;
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

    @Override
    public void onBackPressed() {
        if (mSidebarView != null && mSidebarView.getVisibility() == View.VISIBLE) {
            hideSidebar();
        } else {
            super.onBackPressed();
        }
    }
}
