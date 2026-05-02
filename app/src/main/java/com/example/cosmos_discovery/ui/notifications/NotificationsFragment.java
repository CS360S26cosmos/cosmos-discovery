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
import com.example.cosmos_discovery.model.Notification;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private NotificationAdapter  mAdapter;
    private NotificationService  mService;
    private ListenerRegistration mListener;
    private View                 mEmptyState;
    private View                 mOrganizerComingSoon;
    private RecyclerView         mRv;
    private String               mUid;
    private List<Notification>   mCurrentNotifications = new ArrayList<>();
    private boolean              mShowingPersonal = true;

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
        mRv                  = view.findViewById(R.id.rvGeneralNotifications);

        mAdapter = new NotificationAdapter(requireContext());
        mRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRv.setAdapter(mAdapter);

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
            refreshView();
        }, err -> {
            Log.e("NotificationsFragment", "Listener error: " + err);
            Toast.makeText(requireContext(), "Notifications error: " + err, Toast.LENGTH_LONG).show();
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
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());
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

    private void refreshView() {
        if (mShowingPersonal) {
            mOrganizerComingSoon.setVisibility(View.GONE);
            mRv.setVisibility(View.VISIBLE);
            List<Object> grouped = groupByDate(mCurrentNotifications);
            mAdapter.updateData(grouped);
            mEmptyState.setVisibility(mCurrentNotifications.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            mRv.setVisibility(View.GONE);
            mEmptyState.setVisibility(View.GONE);
            mOrganizerComingSoon.setVisibility(View.VISIBLE);
        }
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
    public void onDestroyView() {
        super.onDestroyView();
        if (mListener != null) {
            mListener.remove();
            mListener = null;
        }
    }
}
