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
import androidx.recyclerview.widget.LinearLayoutManager;
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

    // Pending section
    private View                  mSectionPending;
    private RecyclerView          mRvPending;
    private OrganizerEventAdapter mPendingAdapter;
    private TextView              mTvEmptyPending;
    private TextView              mBadgePendingCount;

    // Approved section
    private View                  mSectionApproved;
    private RecyclerView          mRvApproved;
    private OrganizerEventAdapter mApprovedAdapter;
    private TextView              mTvEmptyApproved;
    private TextView              mBadgeApprovedCount;

    // Rejected section
    private View                  mSectionRejected;
    private View                  mRejectedHeader;
    private View                  mRejectedContent;
    private ImageView             mIconRejectedChevron;
    private RecyclerView          mRvRejected;
    private OrganizerEventAdapter mRejectedAdapter;
    private TextView              mBadgeRejectedCount;
    private boolean               mRejectedExpanded = false;

    // Global empty state
    private TextView mTvEmpty;

    private View mSidebarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        if (RoleUtil.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupAdapters();
        setupTopBar();
        setupSidebar();
        wireBottomNav();
        wireRejectedToggle();
        wireFab();
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

    // ── Setup ─────────────────────────────────────────────────────────────

    private void bindViews() {
        // Pending
        mSectionPending    = findViewById(R.id.sectionPending);
        mRvPending         = findViewById(R.id.rvPendingEvents);
        mTvEmptyPending    = findViewById(R.id.tvEmptyPending);
        mBadgePendingCount = findViewById(R.id.badgePendingCount);

        // Approved
        mSectionApproved    = findViewById(R.id.sectionApproved);
        mRvApproved         = findViewById(R.id.rvApprovedEvents);
        mTvEmptyApproved    = findViewById(R.id.tvEmptyApproved);
        mBadgeApprovedCount = findViewById(R.id.badgeApprovedCount);

        // Rejected
        mSectionRejected     = findViewById(R.id.sectionRejected);
        mRejectedHeader      = findViewById(R.id.rejectedHeader);
        mRejectedContent     = findViewById(R.id.rejectedContent);
        mIconRejectedChevron = findViewById(R.id.iconRejectedChevron);
        mRvRejected          = findViewById(R.id.rvRejectedEvents);
        mBadgeRejectedCount  = findViewById(R.id.badgeRejectedCount);

        // Global empty
        mTvEmpty     = findViewById(R.id.tvEmptyPostedEvents);
        mSidebarView = findViewById(R.id.sidebarView);
    }

    private void setupAdapters() {
        // Pending — small cards in horizontal scroll, blue accent
        mPendingAdapter = new OrganizerEventAdapter(
                this, new ArrayList<>(),
                R.drawable.bg_accent_border_pending,
                true, this::onEventClick);
        mRvPending.setAdapter(mPendingAdapter);

        // Approved — small cards in horizontal scroll, green accent
        mApprovedAdapter = new OrganizerEventAdapter(
                this, new ArrayList<>(),
                R.drawable.bg_accent_border_approved,
                true, this::onEventClick);
        mRvApproved.setAdapter(mApprovedAdapter);

        // Rejected — small cards vertical, dark orange-red accent
        mRejectedAdapter = new OrganizerEventAdapter(
                this, new ArrayList<>(),
                R.drawable.bg_accent_border_rejected,
                this::onEventClick);
        mRvRejected.setAdapter(mRejectedAdapter);
    }

    private void wireRejectedToggle() {
        mRejectedHeader.setOnClickListener(v -> {
            mRejectedExpanded = !mRejectedExpanded;
            mRejectedContent.setVisibility(mRejectedExpanded ? View.VISIBLE : View.GONE);
            mIconRejectedChevron.animate()
                    .rotation(mRejectedExpanded ? 180f : 0f)
                    .setDuration(200)
                    .start();
        });
    }

    private void wireFab() {
        findViewById(R.id.fabCreateEvent).setOnClickListener(v ->
                startActivity(new Intent(this, AddEventActivity.class)));
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private void attachListener() {
        if (mEventsListener != null) return;
        String uid = RoleUtil.getCurrentUser() != null ? RoleUtil.getCurrentUser().getUid() : null;
        if (uid == null || uid.trim().isEmpty()) return;

        Log.d(TAG, "attachListener: querying organizerId=" + uid);

        mEventsListener = mEventService.listenOrganizerEvents(
                uid,
                this::onEventsUpdate,
                err -> {
                    if (err.contains("FAILED_PRECONDITION")) {
                        Log.w(TAG, err);
                        Toast.makeText(this, "Events are loading. Please try again shortly.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void onEventsUpdate(List<Event> events) {
        Log.d(TAG, "onEventsUpdate: received " + (events == null ? "null" : events.size()) + " events");
        if (events != null) {
            for (Event e : events) {
                Log.d(TAG, "  event: title=" + e.getTitle()
                        + " status=" + e.getStatus()
                        + " organizerId=" + e.getOrganizerId()
                        + " id=" + e.getId());
            }
        }

        List<Event> pending  = new ArrayList<>();
        List<Event> approved = new ArrayList<>();
        List<Event> rejected = new ArrayList<>();

        if (events != null) {
            for (Event e : events) {
                String status = e.getStatus();
                if (Event.STATUS_APPROVED.equals(status)) {
                    approved.add(e);
                } else if (Event.STATUS_REJECTED.equals(status)) {
                    rejected.add(e);
                } else {
                    // Default: treat null/unknown as pending
                    pending.add(e);
                }
            }
        }

        Log.d(TAG, "Split: " + pending.size() + " pending, "
                + approved.size() + " approved, " + rejected.size() + " rejected");

        // Sort: pending & approved by dateTime ASC (soonest first)
        pending.sort((a, b) -> Long.compare(a.getDateTime(), b.getDateTime()));
        approved.sort((a, b) -> Long.compare(a.getDateTime(), b.getDateTime()));
        // Rejected by createdAt DESC (most recent first)
        rejected.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        // Update adapters
        mPendingAdapter.updateData(pending);
        mApprovedAdapter.updateData(approved);
        mRejectedAdapter.updateData(rejected);

        // Pending section visibility
        boolean hasPending = !pending.isEmpty();
        mRvPending.setVisibility(hasPending ? View.VISIBLE : View.GONE);
        mTvEmptyPending.setVisibility(hasPending ? View.GONE : View.VISIBLE);
        mBadgePendingCount.setText(String.valueOf(pending.size()));

        // Approved section visibility
        boolean hasApproved = !approved.isEmpty();
        mRvApproved.setVisibility(hasApproved ? View.VISIBLE : View.GONE);
        mTvEmptyApproved.setVisibility(hasApproved ? View.GONE : View.VISIBLE);
        mBadgeApprovedCount.setText(String.valueOf(approved.size()));

        // Rejected section — always visible, show count (including 0)
        mSectionRejected.setVisibility(View.VISIBLE);
        mBadgeRejectedCount.setText(String.valueOf(rejected.size()));
        // Show empty text inside rejected content when expanded but no events
        TextView tvEmptyRejected = findViewById(R.id.tvEmptyRejected);
        if (tvEmptyRejected != null) {
            tvEmptyRejected.setVisibility(rejected.isEmpty() ? View.VISIBLE : View.GONE);
        }
        mRvRejected.setVisibility(rejected.isEmpty() ? View.GONE : View.VISIBLE);

        // Global empty state — only when ALL three are empty
        boolean allEmpty = pending.isEmpty() && approved.isEmpty() && rejected.isEmpty();
        mTvEmpty.setVisibility(allEmpty ? View.VISIBLE : View.GONE);
    }

    private void onEventClick(Event event) {
        if (event == null || event.getId() == null) return;
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    // ── Top bar & sidebar (unchanged) ─────────────────────────────────────

    private void setupTopBar() {
        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.textTitle);
        if (title != null) title.setText("My Posted Events");

        View iconMenu = findViewById(R.id.iconMenu);
        if (iconMenu != null) iconMenu.setOnClickListener(v -> showSidebar());
    }

    private void setupSidebar() {
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

    // ── Sidebar animation ────────────────────────────────────────────────

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
