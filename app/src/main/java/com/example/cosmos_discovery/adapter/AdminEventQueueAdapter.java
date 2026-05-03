package com.example.cosmos_discovery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Event;
import com.example.cosmos_discovery.util.EventStatusUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Admin queue adapter. Renders one card per event with approve/reject actions
 * (Pending tab only) or a status chip + admin-cancel option (Approved tab).
 */
public class AdminEventQueueAdapter
        extends RecyclerView.Adapter<AdminEventQueueAdapter.VH> {

    public interface Listener {
        void onApprove(Event event);
        void onReject(Event event);
        void onCancel(Event event);
    }

    public enum Mode { PENDING, APPROVED, REJECTED }

    private final Context  mContext;
    private final Listener mListener;
    private Mode           mMode;
    private final List<Event> mItems = new ArrayList<>();

    public AdminEventQueueAdapter(Context context, Mode initialMode, Listener listener) {
        mContext  = context;
        mMode     = initialMode;
        mListener = listener;
    }

    public void setMode(Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            notifyDataSetChanged();
        }
    }

    public void updateData(List<Event> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event_queue, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Event e = mItems.get(position);

        h.title.setText(e.getTitle());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d • h:mma", Locale.getDefault());
        h.meta.setText(sdf.format(new Date(e.getDateTime())) + "  •  " + safeStr(e.getLocation()));
        String organizer = e.getOrganizerName();
        h.organizer.setText(organizer != null && !organizer.isEmpty()
                ? "Organizer: " + organizer : "Organizer ID: " + safeStr(e.getOrganizerId()));

        switch (mMode) {
            case PENDING:
                h.btnApprove.setVisibility(View.VISIBLE);
                h.btnReject .setVisibility(View.VISIBLE);
                h.btnCancel .setVisibility(View.GONE);
                h.statusChip.setVisibility(View.GONE);
                h.btnApprove.setOnClickListener(v -> mListener.onApprove(e));
                h.btnReject .setOnClickListener(v -> mListener.onReject(e));
                break;
            case APPROVED:
                h.btnApprove.setVisibility(View.GONE);
                h.btnReject .setVisibility(View.GONE);
                boolean canCancel = e.getDateTime() > System.currentTimeMillis()
                        && !Event.STATUS_CANCELLED.equals(e.getStatus());
                h.btnCancel .setVisibility(canCancel ? View.VISIBLE : View.GONE);
                h.btnCancel .setOnClickListener(v -> mListener.onCancel(e));
                bindStatusChip(h, e.getStatus());
                break;
            case REJECTED:
                h.btnApprove.setVisibility(View.GONE);
                h.btnReject .setVisibility(View.GONE);
                h.btnCancel .setVisibility(View.GONE);
                bindStatusChip(h, e.getStatus());
                break;
        }
    }

    private void bindStatusChip(VH h, String status) {
        h.statusChip.setVisibility(View.VISIBLE);
        h.statusChip.setText(EventStatusUtil.toDisplayText(status));
        h.statusChip.setTextColor(ContextCompat.getColor(mContext, EventStatusUtil.toColorRes(status)));
    }

    private String safeStr(String s) { return s == null ? "" : s; }

    @Override
    public int getItemCount() { return mItems.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, meta, organizer, statusChip;
        final Button   btnApprove, btnReject, btnCancel;
        VH(@NonNull View v) {
            super(v);
            title      = v.findViewById(R.id.tvAdminEventTitle);
            meta       = v.findViewById(R.id.tvAdminEventMeta);
            organizer  = v.findViewById(R.id.tvAdminEventOrganizer);
            statusChip = v.findViewById(R.id.tvAdminEventStatus);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject  = v.findViewById(R.id.btnReject);
            btnCancel  = v.findViewById(R.id.btnAdminCancel);
        }
    }
}
