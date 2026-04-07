package com.example.cosmos_discovery.ui.organizer;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.AttendeeAdapter;
import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AttendeeListActivity extends AppCompatActivity
        implements AttendeeAdapter.OnCheckInToggleListener {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private static final int FILTER_ALL     = 0;
    private static final int FILTER_ARRIVED = 1;
    private static final int FILTER_PENDING = 2;

    private final EventService mEventService = new EventService();
    private final AuthService  mAuthService  = new AuthService();

    private String              mEventId;
    private ListenerRegistration mEventListener;

    // Cached user objects keyed by UID — avoids re-fetching on every snapshot.
    private final Map<String, User> mUserCache = new HashMap<>();

    // Current full attendee list (from cache) and checked-in set.
    private List<User>   mAllAttendees  = new ArrayList<>();
    private Set<String>  mCheckedInIds  = new HashSet<>();

    private int    mActiveFilter = FILTER_ALL;
    private String mSearchQuery  = "";
    private int    mCapacity     = 0;

    // Views
    private TextView         tvRsvpCount;
    private TextView         tvCheckedInCount;
    private TextView         tvPendingCount;
    private MaterialButton   chipAll;
    private MaterialButton   chipArrived;
    private MaterialButton   chipPending;
    private RecyclerView     rvAttendees;
    private TextView         tvEmpty;
    private AttendeeAdapter  mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_list);

        mEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupSearch();
        setupFilterChips();
        setupRecyclerView();
        startListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEventListener != null) {
            mEventListener.remove();
        }
    }

    // ── Initialization ───────────────────────────────────────────────────

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvRsvpCount      = findViewById(R.id.tvRsvpCount);
        tvCheckedInCount = findViewById(R.id.tvCheckedInCount);
        tvPendingCount   = findViewById(R.id.tvPendingCount);

        chipAll     = findViewById(R.id.chipAll);
        chipArrived = findViewById(R.id.chipArrived);
        chipPending = findViewById(R.id.chipPending);

        rvAttendees = findViewById(R.id.rvAttendees);
        tvEmpty     = findViewById(R.id.tvEmpty);
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                mSearchQuery = s.toString().trim();
                applyFilters();
            }
        });
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> setActiveFilter(FILTER_ALL));
        chipArrived.setOnClickListener(v -> setActiveFilter(FILTER_ARRIVED));
        chipPending.setOnClickListener(v -> setActiveFilter(FILTER_PENDING));
        highlightActiveChip();
    }

    private void setupRecyclerView() {
        mAdapter = new AttendeeAdapter(this, new ArrayList<>(), new HashSet<>(), true, this);
        rvAttendees.setLayoutManager(new LinearLayoutManager(this));
        rvAttendees.setAdapter(mAdapter);
    }

    // ── Real-time Listener ───────────────────────────────────────────────

    private void startListening() {
        mEventListener = mEventService.listenEvent(
                mEventId,
                this::onEventUpdate,
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }

    private void onEventUpdate(Event event) {
        mCapacity = event.getCapacity();
        List<String> attendeeIds = event.getAttendeeIds();
        if (attendeeIds == null) attendeeIds = new ArrayList<>();

        List<String> checkedIn = event.getCheckedInIds();
        mCheckedInIds = checkedIn != null ? new HashSet<>(checkedIn) : new HashSet<>();

        // Find new UIDs not yet in cache.
        List<String> newUids = new ArrayList<>();
        for (String uid : attendeeIds) {
            if (!mUserCache.containsKey(uid)) {
                newUids.add(uid);
            }
        }

        // Build current attendee list from cache (for already-known users).
        List<String> finalAttendeeIds = attendeeIds;
        if (newUids.isEmpty()) {
            rebuildAttendeeList(finalAttendeeIds);
            return;
        }

        // Fetch new users, then rebuild.
        final int[] remaining = {newUids.size()};
        for (String uid : newUids) {
            mAuthService.fetchUserDocument(uid,
                    user -> {
                        mUserCache.put(uid, user);
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            rebuildAttendeeList(finalAttendeeIds);
                        }
                    },
                    err -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            rebuildAttendeeList(finalAttendeeIds);
                        }
                    }
            );
        }
    }

    private void rebuildAttendeeList(List<String> attendeeIds) {
        mAllAttendees = new ArrayList<>();
        for (String uid : attendeeIds) {
            User user = mUserCache.get(uid);
            if (user != null) {
                mAllAttendees.add(user);
            }
        }
        updateStatCards();
        applyFilters();
    }

    // ── Stat Cards ───────────────────────────────────────────────────────

    private void updateStatCards() {
        int total     = mAllAttendees.size();
        int checkedIn = mCheckedInIds.size();
        int pending   = total - checkedIn;
        if (pending < 0) pending = 0;

        tvRsvpCount.setText(mCapacity > 0 ? total + "/" + mCapacity : String.valueOf(total));
        tvCheckedInCount.setText(String.valueOf(checkedIn));
        tvPendingCount.setText(String.valueOf(pending));
    }

    // ── Filtering ────────────────────────────────────────────────────────

    private void setActiveFilter(int filter) {
        mActiveFilter = filter;
        highlightActiveChip();
        applyFilters();
    }

    private void applyFilters() {
        List<User> filtered = new ArrayList<>();
        for (User user : mAllAttendees) {
            // Search filter.
            if (!mSearchQuery.isEmpty()) {
                String name = user.getName() != null ? user.getName() : "";
                if (!name.toLowerCase(Locale.ROOT).contains(mSearchQuery.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }
            // Chip filter.
            boolean arrived = mCheckedInIds.contains(user.getUid());
            if (mActiveFilter == FILTER_ARRIVED && !arrived) continue;
            if (mActiveFilter == FILTER_PENDING && arrived)  continue;

            filtered.add(user);
        }

        mAdapter.updateData(filtered, mCheckedInIds);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvAttendees.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

        // Update chip labels with counts.
        int arrivedCount = 0;
        for (User u : mAllAttendees) {
            if (mCheckedInIds.contains(u.getUid())) arrivedCount++;
        }
        int pendingCount = mAllAttendees.size() - arrivedCount;
        chipAll.setText("All (" + mAllAttendees.size() + ")");
        chipArrived.setText("Arrived (" + arrivedCount + ")");
        chipPending.setText("Pending (" + pendingCount + ")");
    }

    private void highlightActiveChip() {
        styleChip(chipAll,     mActiveFilter == FILTER_ALL);
        styleChip(chipArrived, mActiveFilter == FILTER_ARRIVED);
        styleChip(chipPending, mActiveFilter == FILTER_PENDING);
    }

    private void styleChip(MaterialButton chip, boolean active) {
        if (active) {
            chip.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.color_primary)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.color_surface)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.color_text_hint));
        }
    }

    // ── Check-in Toggle (adapter callback) ───────────────────────────────

    @Override
    public void onToggle(String userId, boolean isNowCheckedIn) {
        // Optimistic local update.
        if (isNowCheckedIn) {
            mCheckedInIds.add(userId);
        } else {
            mCheckedInIds.remove(userId);
        }
        updateStatCards();
        applyFilters();

        // Firestore write.
        Runnable onSuccess = () -> {};
        java.util.function.Consumer<String> onFailure = err -> {
            // Rollback.
            if (isNowCheckedIn) {
                mCheckedInIds.remove(userId);
            } else {
                mCheckedInIds.add(userId);
            }
            updateStatCards();
            applyFilters();
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        };

        if (isNowCheckedIn) {
            mEventService.checkInAttendee(mEventId, userId, onSuccess, onFailure);
        } else {
            mEventService.removeCheckIn(mEventId, userId, onSuccess, onFailure);
        }
    }
}
