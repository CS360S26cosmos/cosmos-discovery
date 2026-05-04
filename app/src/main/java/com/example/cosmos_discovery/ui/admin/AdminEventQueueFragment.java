package com.example.cosmos_discovery.ui.admin;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.AdminEventQueueAdapter;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.database.NotificationService;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.model.Notification;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Admin event approval queue. Pending events get Approve / Reject (with optional reason).
 * Approved events can be force-cancelled. Rejected events are read-only.
 */
public class AdminEventQueueFragment extends Fragment {

    private final EventService        mEventService = new EventService();
    private final NotificationService mNotifService = new NotificationService();

    private TextView                mChipPending, mChipApproved, mChipRejected;
    private RecyclerView            mRv;
    private TextView                mEmpty;
    private AdminEventQueueAdapter  mAdapter;
    private AdminEventQueueAdapter.Mode mCurrentMode = AdminEventQueueAdapter.Mode.PENDING;
    private ListenerRegistration    mListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_event_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mChipPending  = view.findViewById(R.id.chipPending);
        mChipApproved = view.findViewById(R.id.chipApproved);
        mChipRejected = view.findViewById(R.id.chipRejected);
        mRv           = view.findViewById(R.id.rvAdminEvents);
        mEmpty        = view.findViewById(R.id.tvEmptyAdminEvents);

        mAdapter = new AdminEventQueueAdapter(requireContext(), mCurrentMode, new AdminEventQueueAdapter.Listener() {
            @Override public void onApprove(Event event) { confirmApprove(event); }
            @Override public void onReject (Event event) { confirmReject(event);  }
            @Override public void onCancel (Event event) { confirmCancel(event);  }
        });
        mRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRv.setAdapter(mAdapter);

        mChipPending .setOnClickListener(v -> selectMode(AdminEventQueueAdapter.Mode.PENDING));
        mChipApproved.setOnClickListener(v -> selectMode(AdminEventQueueAdapter.Mode.APPROVED));
        mChipRejected.setOnClickListener(v -> selectMode(AdminEventQueueAdapter.Mode.REJECTED));

        selectMode(AdminEventQueueAdapter.Mode.PENDING);
    }

    private void selectMode(AdminEventQueueAdapter.Mode mode) {
        mCurrentMode = mode;
        mAdapter.setMode(mode);
        styleChip(mChipPending,  mode == AdminEventQueueAdapter.Mode.PENDING);
        styleChip(mChipApproved, mode == AdminEventQueueAdapter.Mode.APPROVED);
        styleChip(mChipRejected, mode == AdminEventQueueAdapter.Mode.REJECTED);
        attachListener(mode);
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.white : R.color.color_text_secondary));
    }

    private void attachListener(AdminEventQueueAdapter.Mode mode) {
        if (mListener != null) {
            mListener.remove();
            mListener = null;
        }
        String status;
        switch (mode) {
            case APPROVED: status = Event.STATUS_APPROVED; break;
            case REJECTED: status = Event.STATUS_REJECTED; break;
            default:       status = Event.STATUS_PENDING;  break;
        }

        mListener = FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("status", status)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(requireContext(),
                                "Could not load events: " + err.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Event e = doc.toObject(Event.class);
                            if (e != null) {
                                e.setId(doc.getId());
                                events.add(e);
                            }
                        }
                    }
                    Collections.sort(events, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    mAdapter.updateData(events);
                    boolean empty = events.isEmpty();
                    mEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    mEmpty.setText(emptyTextFor(mode));
                });
    }

    private String emptyTextFor(AdminEventQueueAdapter.Mode mode) {
        switch (mode) {
            case APPROVED: return "No approved events.";
            case REJECTED: return "No rejected events.";
            default:       return "No pending events to review.";
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void confirmApprove(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Approve event?")
                .setMessage("Approve \"" + event.getTitle() + "\"? It will become visible to all students.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Approve", (d, w) -> applyDecision(event, true, null))
                .show();
    }

    private void confirmReject(Event event) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reject_event, null, false);

        TextView tvSubtitle = dialogView.findViewById(R.id.tvRejectSubtitle);
        tvSubtitle.setText("Reject \"" + event.getTitle() + "\"?");

        TextInputEditText etReason = dialogView.findViewById(R.id.etRejectReason);

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogView.findViewById(R.id.btnRejectCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnRejectConfirm).setOnClickListener(v -> {
            String reason = etReason.getText() != null
                    ? etReason.getText().toString().trim() : "";
            dialog.dismiss();
            applyDecision(event, false, reason);
        });

        dialog.show();
    }

    private void confirmCancel(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel event?")
                .setMessage("Force-cancel \"" + event.getTitle() + "\"? "
                        + "The organizer and all RSVP'd attendees will be notified.")
                .setNegativeButton("Keep event", null)
                .setPositiveButton("Cancel event", (d, w) -> {
                    String adminUid = RoleUtil.getCurrentUser() != null
                            ? RoleUtil.getCurrentUser().getUid() : null;
                    mEventService.cancelEvent(event.getId(), adminUid,
                            () -> Toast.makeText(requireContext(), "Event cancelled.", Toast.LENGTH_SHORT).show(),
                            err -> Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private void applyDecision(Event event, boolean approved, String rejectReason) {
        String newStatus = approved ? Event.STATUS_APPROVED : Event.STATUS_REJECTED;
        if (approved) {
            mEventService.updateEventStatus(event.getId(), newStatus,
                    () -> {
                        notifyOrganizer(event, true, null);
                        Toast.makeText(requireContext(), "Event approved.", Toast.LENGTH_SHORT).show();
                    },
                    err -> Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show());
        } else {
            mEventService.updateEventStatusWithReason(event.getId(), newStatus, rejectReason,
                    () -> {
                        notifyOrganizer(event, false, rejectReason);
                        Toast.makeText(requireContext(), "Event rejected.", Toast.LENGTH_SHORT).show();
                    },
                    err -> Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show());
        }
    }

    private void notifyOrganizer(Event event, boolean approved, String rejectReason) {
        if (event.getOrganizerId() == null || event.getOrganizerId().isEmpty()) return;

        String title;
        String body;
        String type;
        if (approved) {
            title = "Event approved";
            body  = "\"" + event.getTitle() + "\" was approved and is now live.";
            type  = Notification.TYPE_EVENT_APPROVED;
        } else {
            title = "Event rejected";
            body  = "\"" + event.getTitle() + "\" was rejected.";
            if (rejectReason != null && !rejectReason.isEmpty()) {
                body += " Reason: " + rejectReason;
            }
            type = Notification.TYPE_EVENT_REJECTED;
        }
        Notification n = new Notification(type, title, body, System.currentTimeMillis());
        n.setEventId(event.getId());
        n.setEventTitle(event.getTitle());
        n.setAudience(Notification.AUDIENCE_ORGANIZER);
        // Fixed doc ID per (event, decision) so re-decisions overwrite.
        String docId = (approved ? "event_approved_" : "event_rejected_") + event.getId();
        mNotifService.writeNotificationToUsers(
                Collections.singletonList(event.getOrganizerId()),
                docId, n, () -> {}, err -> {});
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
