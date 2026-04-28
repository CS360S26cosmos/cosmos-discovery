package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.IncomingRequestAdapter;
import com.example.cosmos_discovery.adapter.SentRequestAdapter;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.database.FriendService;
import com.example.cosmos_discovery.model.FriendEntry;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.ui.auth.LoginActivity;
import com.example.cosmos_discovery.ui.organizer.AddEventActivity;
import com.example.cosmos_discovery.ui.organizer.OrganizerActivity;
import com.example.cosmos_discovery.util.DismissedEventsStore;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FriendRequestsActivity extends AppCompatActivity {

    private View mSidebarView;
    private final AuthService  mAuthService  = new AuthService();
    private final FriendService mFriendService = new FriendService();

    private RecyclerView mRvIncoming;
    private RecyclerView mRvSent;
    private TextView mBadgeIncomingCount;
    private TextView mTvEmptyIncoming;
    private TextView mTvEmptySent;
    private IncomingRequestAdapter mIncomingAdapter;
    private SentRequestAdapter     mSentAdapter;

    private ListenerRegistration mIncomingListener;
    private ListenerRegistration mSentListener;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        mSidebarView = findViewById(R.id.sidebarView);

        bindViews();
        setupAdapters();
        setupTopBar();
        setupSidebar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mIncomingListener != null) {
            mIncomingListener.remove();
            mIncomingListener = null;
        }
        if (mSentListener != null) {
            mSentListener.remove();
            mSentListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        User user = RoleUtil.getCurrentUser();
        if (user != null && mSidebarView != null) {
            TextView name = mSidebarView.findViewById(R.id.sidebarUserName);
            name.setText(user.getName());
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                ShapeableImageView icon = mSidebarView.findViewById(R.id.sidebarProfileIcon);
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_sidebar_main_profileimage)
                        .centerCrop()
                        .into(icon);
            }
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    private void bindViews() {
        mRvIncoming         = findViewById(R.id.rvIncomingRequests);
        mRvSent             = findViewById(R.id.rvSentRequests);
        mBadgeIncomingCount = findViewById(R.id.badgeIncomingCount);
        mTvEmptyIncoming    = findViewById(R.id.tvEmptyIncoming);
        mTvEmptySent        = findViewById(R.id.tvEmptySent);
    }

    private void setupAdapters() {
        mIncomingAdapter = new IncomingRequestAdapter(new ArrayList<>(),
                new IncomingRequestAdapter.OnActionListener() {
                    @Override
                    public void onAccept(IncomingRequestAdapter.RequestItem item) {
                        User current = RoleUtil.getCurrentUser();
                        if (current == null) return;
                        mFriendService.acceptRequest(
                                current, item.uid, item.name, item.photoUrl,
                                () -> { /* listener auto-refreshes the list */ },
                                err -> Toast.makeText(
                                        FriendRequestsActivity.this, err, Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onDecline(IncomingRequestAdapter.RequestItem item) {
                        User current = RoleUtil.getCurrentUser();
                        if (current == null) return;
                        mFriendService.declineRequest(
                                current.getUid(), item.uid,
                                () -> { /* listener auto-refreshes */ },
                                err -> Toast.makeText(
                                        FriendRequestsActivity.this, err, Toast.LENGTH_SHORT).show());
                    }
                });
        mRvIncoming.setLayoutManager(new LinearLayoutManager(this));
        mRvIncoming.setAdapter(mIncomingAdapter);
        mRvIncoming.setNestedScrollingEnabled(false);

        mSentAdapter = new SentRequestAdapter(new ArrayList<>(), item -> {
            User current = RoleUtil.getCurrentUser();
            if (current == null) return;
            mFriendService.cancelSentRequest(
                    current.getUid(), item.uid,
                    () -> { /* listener auto-refreshes */ },
                    err -> Toast.makeText(
                            FriendRequestsActivity.this, err, Toast.LENGTH_SHORT).show());
        });
        mRvSent.setLayoutManager(new LinearLayoutManager(this));
        mRvSent.setAdapter(mSentAdapter);
        mRvSent.setNestedScrollingEnabled(false);

        mBadgeIncomingCount.setText("0");
        mTvEmptyIncoming.setVisibility(View.VISIBLE);
        mTvEmptySent.setVisibility(View.VISIBLE);
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.textTitle);
        if (title != null) title.setText("Friend Requests");

        View iconMenu = findViewById(R.id.iconMenu);
        if (iconMenu != null) iconMenu.setOnClickListener(v -> showSidebar());
    }

    private void setupSidebar() {
        if (mSidebarView == null) return;

        User user = RoleUtil.getCurrentUser();
        if (user != null) {
            TextView name  = mSidebarView.findViewById(R.id.sidebarUserName);
            TextView email = mSidebarView.findViewById(R.id.sidebarUserEmail);
            name.setText(user.getName());
            email.setText(user.getEmail());
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                ShapeableImageView icon = mSidebarView.findViewById(R.id.sidebarProfileIcon);
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_sidebar_main_profileimage)
                        .centerCrop()
                        .into(icon);
            }
        }

        View organizerSection = mSidebarView.findViewById(R.id.organizerSection);
        if (organizerSection != null) {
            organizerSection.setVisibility(RoleUtil.isOrganizer() ? View.VISIBLE : View.GONE);
        }
        if (RoleUtil.isOrganizer()) {
            View posted = mSidebarView.findViewById(R.id.organizerPostedEventsRow);
            View create = mSidebarView.findViewById(R.id.organizerCreateEventRow);
            if (posted != null) posted.setOnClickListener(v -> {
                hideSidebar();
                startActivity(new Intent(this, OrganizerActivity.class));
            });
            if (create != null) create.setOnClickListener(v -> {
                hideSidebar();
                startActivity(new Intent(this, AddEventActivity.class));
            });
        }

        mSidebarView.findViewById(R.id.sidebarOverlay).setOnClickListener(v -> hideSidebar());
        mSidebarView.findViewById(R.id.btnCloseSidebar).setOnClickListener(v -> hideSidebar());

        View friendRequestsRow = mSidebarView.findViewById(R.id.friendRequestsRow);
        if (friendRequestsRow != null) {
            friendRequestsRow.setOnClickListener(v -> hideSidebar());
        }

        mSidebarView.findViewById(R.id.profileRow).setOnClickListener(v -> {
            hideSidebar();
            startActivity(new Intent(this, ViewProfile.class));
        });

        mSidebarView.findViewById(R.id.settingsRow).setOnClickListener(v -> {
            hideSidebar();
            startActivity(new Intent(this,
                    com.example.cosmos_discovery.ui.shared.SettingsActivity.class));
        });

        mSidebarView.findViewById(R.id.logoutRow).setOnClickListener(v -> {
            mAuthService.signOut();
            new DismissedEventsStore(this).clear();
            RoleUtil.clear();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Data ──────────────────────────────────────────────────────────────

    private void attachListeners() {
        User current = RoleUtil.getCurrentUser();
        if (current == null) return;
        String uid = current.getUid();

        if (mIncomingListener == null) {
            mIncomingListener = mFriendService.listenIncomingRequests(uid,
                    this::onIncomingUpdate,
                    err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
        }
        if (mSentListener == null) {
            mSentListener = mFriendService.listenSentRequests(uid,
                    this::onSentUpdate,
                    err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
        }
    }

    private void onIncomingUpdate(List<FriendEntry> entries) {
        List<IncomingRequestAdapter.RequestItem> items = new ArrayList<>();
        for (FriendEntry e : entries) {
            String name     = e.getName() != null ? e.getName() : "";
            String initials = name.length() >= 2
                    ? name.substring(0, 2).toUpperCase(Locale.getDefault())
                    : name.toUpperCase(Locale.getDefault());
            items.add(new IncomingRequestAdapter.RequestItem(
                    e.getUid(), name, "", initials, e.getPhotoUrl()));
        }
        mIncomingAdapter.updateData(items);
        mBadgeIncomingCount.setText(String.valueOf(items.size()));
        mTvEmptyIncoming.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onSentUpdate(List<FriendEntry> entries) {
        List<SentRequestAdapter.RequestItem> items = new ArrayList<>();
        for (FriendEntry e : entries) {
            String name     = e.getName() != null ? e.getName() : "";
            String initials = name.length() >= 2
                    ? name.substring(0, 2).toUpperCase(Locale.getDefault())
                    : name.toUpperCase(Locale.getDefault());
            String statusLine = "Pending · " + formatSentTime(e.getAddedAt());
            items.add(new SentRequestAdapter.RequestItem(
                    e.getUid(), name, statusLine, initials, e.getPhotoUrl()));
        }
        mSentAdapter.updateData(items);
        mTvEmptySent.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Formats a timestamp into "Sent today", "Sent 1 day ago", "Sent N days ago". */
    private String formatSentTime(long sentAtMs) {
        long days = (System.currentTimeMillis() - sentAtMs) / (1000L * 60 * 60 * 24);
        if (days <= 0) return "Sent today";
        if (days == 1) return "Sent 1 day ago";
        return "Sent " + days + " days ago";
    }

    // ── Sidebar animation ─────────────────────────────────────────────────

    private void showSidebar() {
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
