package com.example.cosmos_discovery.ui.notifications;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.NotificationAdapter;
import com.example.cosmos_discovery.database.NotificationService;
import com.example.cosmos_discovery.database.PreferenceService;
import com.example.cosmos_discovery.model.Notification;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private NotificationAdapter  mAdapter;
    private NotificationService  mService;
    private ListenerRegistration mListener;
    private View                 mEmptyState;
    private View                 mOrganizerComingSoon;
    private View                 mClearAll;
    private RecyclerView         mRv;
    private String               mUid;
    private List<Notification>   mCurrentNotifications = new ArrayList<>();
    private boolean              mShowingPersonal = true;
    private Map<String, Boolean> mPrefs = new HashMap<>();
    private final PreferenceService mPrefService = new PreferenceService();

    /** Mirrors the Cloud Function's TYPE_TO_PREF map so the UI hides what wouldn't push. */
    private static final Map<String, String> TYPE_TO_PREF = new HashMap<>();
    static {
        TYPE_TO_PREF.put(Notification.TYPE_RSVP_CONFIRMED,          "rsvp");
        TYPE_TO_PREF.put(Notification.TYPE_EVENT_UPDATED,           "eventUpdates");
        TYPE_TO_PREF.put(Notification.TYPE_EVENT_CANCELLED,         "eventUpdates");
        TYPE_TO_PREF.put(Notification.TYPE_ANNOUNCEMENT,            "announcements");
        TYPE_TO_PREF.put(Notification.TYPE_FRIEND_REQUEST_RECEIVED, "friendRequests");
        TYPE_TO_PREF.put(Notification.TYPE_FRIEND_REQUEST_ACCEPTED, "friendRequests");
        TYPE_TO_PREF.put(Notification.TYPE_EVENT_APPROVED,          "adminDecisions");
        TYPE_TO_PREF.put(Notification.TYPE_EVENT_REJECTED,          "adminDecisions");
        TYPE_TO_PREF.put(Notification.TYPE_CAPACITY_FULL,           "capacityFull");
        TYPE_TO_PREF.put(Notification.TYPE_RSVP_RECEIVED,           "rsvpReceived");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyState          = view.findViewById(R.id.tvEmptyGeneralNotifications);
        mOrganizerComingSoon = view.findViewById(R.id.tvOrganizerComingSoon);
        mClearAll            = view.findViewById(R.id.tvClearAll);
        mRv                  = view.findViewById(R.id.rvGeneralNotifications);

        mAdapter = new NotificationAdapter(requireContext());
        mRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRv.setAdapter(mAdapter);

        mClearAll.setOnClickListener(v -> confirmClearAll());

        attachSwipeToDelete();

        // Show toggle and wire it — organizers only
        if (RoleUtil.isOrganizer()) {
            View toggle           = view.findViewById(R.id.toggleNotifType);
            TextView tabPersonal  = view.findViewById(R.id.btnPersonal);
            TextView tabOrganizer = view.findViewById(R.id.btnOrganizer);
            toggle.setVisibility(View.VISIBLE);

            tabPersonal.setOnClickListener(v -> {
                if (mShowingPersonal) return;
                mShowingPersonal = true;
                tabPersonal.setBackgroundResource(R.drawable.bg_notif_tab_selected);
                tabPersonal.setTextColor(getResources().getColor(R.color.white, null));
                tabOrganizer.setBackgroundResource(R.drawable.bg_notif_tab_unselected);
                tabOrganizer.setTextColor(getResources().getColor(R.color.color_text_hint, null));
                refreshView();
            });

            tabOrganizer.setOnClickListener(v -> {
                if (!mShowingPersonal) return;
                mShowingPersonal = false;
                tabOrganizer.setBackgroundResource(R.drawable.bg_notif_tab_selected);
                tabOrganizer.setTextColor(getResources().getColor(R.color.white, null));
                tabPersonal.setBackgroundResource(R.drawable.bg_notif_tab_unselected);
                tabPersonal.setTextColor(getResources().getColor(R.color.color_text_hint, null));
                refreshView();
            });
        }

        if (RoleUtil.getCurrentUser() == null) return;
        mUid = RoleUtil.getCurrentUser().getUid();

        mService  = new NotificationService();
        mListener = mService.listenNotifications(mUid, notifications -> {
            mCurrentNotifications = notifications != null ? notifications : new ArrayList<>();
            // Stamp legacy docs (pre-audience field) with their inferred audience so
            // they show up in the right tab. No-op once they're all stamped.
            mService.backfillAudience(mUid, mCurrentNotifications);
            refreshView();
        }, err -> {
            Log.e("NotificationsFragment", "Listener error: " + err);
            if (isAdded()) Toast.makeText(requireContext(), "Notifications error: " + err, Toast.LENGTH_LONG).show();
        });
    }

    // ── Swipe to delete ───────────────────────────────────────────────────

    private void attachSwipeToDelete() {
        int swipeColor   = ContextCompat.getColor(requireContext(), R.color.color_primary);
        Drawable trashIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh) {
                // Only notification rows are swipeable — not date headers
                if (mAdapter.getItemViewType(vh.getAdapterPosition())
                        == NotificationAdapter.VIEW_TYPE_HEADER) return 0;
                return super.getSwipeDirs(rv, vh);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                Notification notif = mAdapter.getNotificationAt(pos);
                if (notif == null || notif.getId() == null || mUid == null) return;

                mService.deleteNotification(mUid, notif.getId(),
                        () -> { /* real-time listener will refresh the list */ },
                        err -> { if (isAdded()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show(); });
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                View itemView = vh.itemView;
                float cornerRadius = 16f; // ~8dp, matches notification card corners

                // Background rectangle revealed on swipe
                Paint bgPaint = new Paint();
                bgPaint.setColor(swipeColor);
                RectF bg = new RectF(
                        itemView.getRight() + dX,
                        itemView.getTop() + 8f,
                        itemView.getRight(),
                        itemView.getBottom() - 8f);
                c.drawRoundRect(bg, cornerRadius, cornerRadius, bgPaint);

                // Trash icon — centred in the revealed area, max 24dp wide
                if (trashIcon != null) {
                    int iconSize  = (int) (24 * rv.getResources().getDisplayMetrics().density);
                    int iconMargin = (int) (16 * rv.getResources().getDisplayMetrics().density);
                    int iconLeft  = itemView.getRight() - iconMargin - iconSize;
                    int iconTop   = itemView.getTop()
                            + (itemView.getHeight() - iconSize) / 2;
                    trashIcon.setBounds(iconLeft, iconTop,
                            iconLeft + iconSize, iconTop + iconSize);
                    trashIcon.setAlpha(Math.min(255,
                            (int) (255 * Math.abs(dX) / (iconMargin * 2 + iconSize))));
                    trashIcon.draw(c);
                }

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(mRv);
    }

    // ── View logic ────────────────────────────────────────────────────────

    private void confirmClearAll() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear all notifications")
                .setMessage("Remove all notifications? This cannot be undone.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Clear all", (d, w) ->
                        mService.deleteAllNotifications(mUid, mCurrentNotifications,
                                () -> { /* real-time listener auto-refreshes */ },
                                err -> { if (isAdded()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show(); }))
                .show();
    }

    private void refreshView() {
        // The "coming soon" placeholder is no longer used — both tabs render real lists.
        mOrganizerComingSoon.setVisibility(View.GONE);
        mRv.setVisibility(View.VISIBLE);

        List<Notification> filtered = filterByPrefs(mCurrentNotifications);
        List<Notification> visible  = filterByTab(filtered, mShowingPersonal);
        List<Object> grouped = groupByDate(visible);
        mAdapter.updateData(grouped);
        boolean empty = visible.isEmpty();
        mEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        mClearAll.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * Splits notifications by audience perspective:
     *   • Personal — things that happen to YOU (RSVP'd, friend requests, event you attend updated/cancelled, announcements you receive).
     *   • Organizer — things about events YOU run (someone RSVPed, your event filled, admin approved/rejected your event).
     */
    private List<Notification> filterByTab(List<Notification> source, boolean personal) {
        List<Notification> kept = new ArrayList<>(source.size());
        for (Notification n : source) {
            boolean isOrganizerNotif = isOrganizerNotif(n);
            if (personal != isOrganizerNotif) kept.add(n);
        }
        return kept;
    }

    /**
     * Trust the explicit audience field when present (set by all current writers).
     * Falls back to type-based heuristic for any legacy notif docs that pre-date the field.
     */
    private boolean isOrganizerNotif(Notification n) {
        if (Notification.AUDIENCE_ORGANIZER.equals(n.getAudience())) return true;
        if (Notification.AUDIENCE_PERSONAL .equals(n.getAudience())) return false;

        String type = n.getType();
        if (type == null) return false;
        return Notification.TYPE_RSVP_RECEIVED  .equals(type)
            || Notification.TYPE_CAPACITY_FULL  .equals(type)
            || Notification.TYPE_EVENT_APPROVED .equals(type)
            || Notification.TYPE_EVENT_REJECTED .equals(type);
    }

    /**
     * Drops notifications whose category is disabled in user preferences.
     * Master "push" off hides everything. Missing keys are treated as enabled
     * (matches the Cloud Function's behavior).
     */
    private List<Notification> filterByPrefs(List<Notification> source) {
        if (mPrefs == null || mPrefs.isEmpty()) return source;
        Boolean master = mPrefs.get("push");
        if (master != null && !master) return new ArrayList<>();

        List<Notification> kept = new ArrayList<>(source.size());
        for (Notification n : source) {
            String prefKey = TYPE_TO_PREF.get(n.getType());
            if (prefKey != null) {
                Boolean v = mPrefs.get(prefKey);
                if (v != null && !v) continue;
            }
            kept.add(n);
        }
        return kept;
    }

    /** Groups a newest-first list into Today / Yesterday / This Week / Older with String headers. */
    private List<Object> groupByDate(List<Notification> notifications) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart     = cal.getTimeInMillis();
        long yesterdayStart = todayStart - 86_400_000L;
        long weekStart      = todayStart - 6 * 86_400_000L;

        List<Object> result     = new ArrayList<>();
        String       lastHeader = null;

        for (Notification n : notifications) {
            String header;
            if      (n.getTimestamp() >= todayStart)     header = "Today";
            else if (n.getTimestamp() >= yesterdayStart) header = "Yesterday";
            else if (n.getTimestamp() >= weekStart)      header = "This Week";
            else                                         header = "Older";

            if (!header.equals(lastHeader)) {
                result.add(header);
                lastHeader = header;
            }
            result.add(n);
        }
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid == null) return;
        mPrefService.getPrefs(mUid, prefs -> {
            mPrefs = prefs;
            refreshView();
        }, err -> { /* fall back to showing everything */ });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mListener != null) {
            mListener.remove();
            mListener = null;
        }
    }
}
