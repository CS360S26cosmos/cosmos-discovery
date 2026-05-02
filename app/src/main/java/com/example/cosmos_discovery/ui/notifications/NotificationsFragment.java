package com.example.cosmos_discovery.ui.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.NotificationAdapter;
import com.example.cosmos_discovery.database.NotificationService;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

public class NotificationsFragment extends Fragment {

    private NotificationAdapter  mAdapter;
    private NotificationService  mService;
    private ListenerRegistration mListener;
    private View                 mEmptyState;

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

        mEmptyState = view.findViewById(R.id.tvEmptyGeneralNotifications);

        // Show organizer section only for organizers/admins
        View organizerSection = view.findViewById(R.id.layoutOrganizerSection);
        if (RoleUtil.isOrganizer()) {
            organizerSection.setVisibility(View.VISIBLE);
        }

        RecyclerView rv = view.findViewById(R.id.rvGeneralNotifications);
        mAdapter = new NotificationAdapter(requireContext());
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(mAdapter);

        if (RoleUtil.getCurrentUser() == null) return;
        String uid = RoleUtil.getCurrentUser().getUid();

        mService  = new NotificationService();
        mListener = mService.listenNotifications(uid, notifications -> {
            mAdapter.updateData(notifications);
            mEmptyState.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            Log.e("NotificationsFragment", "Listener error: " + err);
            Toast.makeText(requireContext(), "Notifications error: " + err, Toast.LENGTH_LONG).show();
        });
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
