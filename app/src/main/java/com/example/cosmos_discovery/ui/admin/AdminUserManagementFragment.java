package com.example.cosmos_discovery.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.AdminUserAdapter;
import com.example.cosmos_discovery.adapter.OrganizerRequestAdapter;
import com.example.cosmos_discovery.database.AdminService;
import com.example.cosmos_discovery.model.OrganizerRequest;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class AdminUserManagementFragment extends Fragment
        implements OrganizerRequestAdapter.OnRequestActionListener,
                   AdminUserAdapter.OnUserActionListener {

    private AdminService mAdminService;
    private ListenerRegistration mRequestsListener;

    private TextView mChipPending;
    private TextView mChipAllUsers;
    private View     mPanelPending;
    private View     mPanelAllUsers;

    private RecyclerView mRvPending;
    private TextView     mTvEmptyPending;
    private RecyclerView mRvAllUsers;
    private TextView     mTvEmptyUsers;

    private OrganizerRequestAdapter mRequestAdapter;
    private AdminUserAdapter        mUserAdapter;

    // Role change overlay views
    private View     mOverlayRoleChange;
    private TextView mTvRoleChangeUserName;
    private TextView mRoleChipStudent;
    private TextView mRoleChipOrganizer;
    private TextView mRoleChipAdmin;

    // Overlay state
    private User   mPendingRoleUser;
    private String mSelectedRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdminService = new AdminService();

        // Bind views
        mChipPending    = view.findViewById(R.id.chipPendingRequests);
        mChipAllUsers   = view.findViewById(R.id.chipAllUsers);
        mPanelPending   = view.findViewById(R.id.panelPendingRequests);
        mPanelAllUsers  = view.findViewById(R.id.panelAllUsers);
        mRvPending      = view.findViewById(R.id.rvPendingRequests);
        mTvEmptyPending = view.findViewById(R.id.tvEmptyRequests);
        mRvAllUsers     = view.findViewById(R.id.rvAllUsers);
        mTvEmptyUsers   = view.findViewById(R.id.tvEmptyUsers);

        // Set up adapters
        mRequestAdapter = new OrganizerRequestAdapter(this);
        mRvPending.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvPending.setAdapter(mRequestAdapter);

        String adminUid = RoleUtil.getCurrentUser() != null
                ? RoleUtil.getCurrentUser().getUid() : null;
        mUserAdapter = new AdminUserAdapter(adminUid, this);
        mRvAllUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvAllUsers.setAdapter(mUserAdapter);

        // Chip tab listeners
        mChipPending.setOnClickListener(v -> selectTab(true));
        mChipAllUsers.setOnClickListener(v -> selectTab(false));

        // Search bar
        EditText etSearch = view.findViewById(R.id.etSearchUsers);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUserAdapter.setQuery(s.toString());
                updateEmptyUsers();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Role change overlay
        mOverlayRoleChange    = view.findViewById(R.id.overlayRoleChange);
        mTvRoleChangeUserName = view.findViewById(R.id.tvRoleChangeUserName);
        mRoleChipStudent      = view.findViewById(R.id.chipRoleStudent);
        mRoleChipOrganizer    = view.findViewById(R.id.chipRoleOrganizer);
        mRoleChipAdmin        = view.findViewById(R.id.chipRoleAdmin);

        mOverlayRoleChange.setOnClickListener(v -> dismissRoleChangeOverlay());
        view.findViewById(R.id.roleChangeCard).setOnClickListener(v -> { /* consume backdrop tap */ });
        view.findViewById(R.id.btnCloseRoleChange).setOnClickListener(v -> dismissRoleChangeOverlay());
        view.findViewById(R.id.btnCancelRoleChange).setOnClickListener(v -> dismissRoleChangeOverlay());

        mRoleChipStudent.setOnClickListener(v -> selectRoleChip(mRoleChipStudent, User.ROLE_STUDENT));
        mRoleChipOrganizer.setOnClickListener(v -> selectRoleChip(mRoleChipOrganizer, User.ROLE_ORGANIZER));
        mRoleChipAdmin.setOnClickListener(v -> selectRoleChip(mRoleChipAdmin, User.ROLE_ADMIN));

        view.findViewById(R.id.btnConfirmRoleChange).setOnClickListener(v -> confirmRoleChange());

        // Start on Pending Requests tab
        selectTab(true);

        // Load data
        startListeningPendingRequests();
        loadAllUsers();
    }

    private void selectTab(boolean pendingSelected) {
        if (pendingSelected) {
            mChipPending.setBackgroundResource(R.drawable.bg_chip_selected);
            mChipPending.setTextColor(requireContext().getColor(R.color.white));
            mChipAllUsers.setBackgroundResource(R.drawable.bg_chip_unselected);
            mChipAllUsers.setTextColor(requireContext().getColor(R.color.color_text_secondary));
            mPanelPending.setVisibility(View.VISIBLE);
            mPanelAllUsers.setVisibility(View.GONE);
        } else {
            mChipAllUsers.setBackgroundResource(R.drawable.bg_chip_selected);
            mChipAllUsers.setTextColor(requireContext().getColor(R.color.white));
            mChipPending.setBackgroundResource(R.drawable.bg_chip_unselected);
            mChipPending.setTextColor(requireContext().getColor(R.color.color_text_secondary));
            mPanelPending.setVisibility(View.GONE);
            mPanelAllUsers.setVisibility(View.VISIBLE);
        }
    }

    private void startListeningPendingRequests() {
        mRequestsListener = mAdminService.listenPendingRequests(
                this::onRequestsUpdated,
                err -> showToast(err));
    }

    private void onRequestsUpdated(List<OrganizerRequest> requests) {
        if (!isAdded()) return;
        mRequestAdapter.updateData(requests);
        mTvEmptyPending.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
        mRvPending.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadAllUsers() {
        mAdminService.fetchAllUsers(
                users -> {
                    if (!isAdded()) return;
                    mUserAdapter.updateData(users);
                    updateEmptyUsers();
                },
                err -> showToast(err));
    }

    private void updateEmptyUsers() {
        boolean empty = mUserAdapter.getItemCount() == 0;
        mTvEmptyUsers.setVisibility(empty ? View.VISIBLE : View.GONE);
        mRvAllUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // -- OrganizerRequestAdapter.OnRequestActionListener -------------------

    @Override
    public void onAccept(OrganizerRequest request) {
        mAdminService.approveRequest(
                request.getUserId(),
                () -> {
                    if (!isAdded()) return;
                    mRequestAdapter.removeItem(request);
                    updateEmptyPending();
                    showToast("Request approved.");
                },
                err -> showToast(err));
    }

    @Override
    public void onReject(OrganizerRequest request) {
        mAdminService.rejectRequest(
                request.getUserId(),
                () -> {
                    if (!isAdded()) return;
                    mRequestAdapter.removeItem(request);
                    updateEmptyPending();
                    showToast("Request rejected.");
                },
                err -> showToast(err));
    }

    private void updateEmptyPending() {
        boolean empty = mRequestAdapter.getItemCount() == 0;
        mTvEmptyPending.setVisibility(empty ? View.VISIBLE : View.GONE);
        mRvPending.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // -- AdminUserAdapter.OnUserActionListener -----------------------------

    @Override
    public void onDeactivate(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate User")
                .setMessage("Deactivate " + (user.getName() != null ? user.getName() : "this user")
                        + "? They will be unable to sign in.")
                .setPositiveButton("Deactivate", (d, w) ->
                        mAdminService.deactivateUser(
                                user.getUid(),
                                () -> {
                                    if (!isAdded()) return;
                                    user.setActive(false);
                                    mUserAdapter.updateUser(user);
                                    showToast("User deactivated.");
                                },
                                err -> showToast(err)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onReactivate(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reactivate User")
                .setMessage("Reactivate " + (user.getName() != null ? user.getName() : "this user")
                        + "? They will be able to sign in again.")
                .setPositiveButton("Reactivate", (d, w) ->
                        mAdminService.reactivateUser(
                                user.getUid(),
                                () -> {
                                    if (!isAdded()) return;
                                    user.setActive(true);
                                    mUserAdapter.updateUser(user);
                                    showToast("User reactivated.");
                                },
                                err -> showToast(err)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onChangeStatus(User user) {
        showRoleChangeOverlay(user);
    }

    // -- Role change overlay -----------------------------------------------

    private void showRoleChangeOverlay(User user) {
        mPendingRoleUser = user;
        mSelectedRole    = null;

        String currentRole = user.getRole();
        mRoleChipStudent.setVisibility(User.ROLE_STUDENT.equals(currentRole)   ? View.GONE : View.VISIBLE);
        mRoleChipOrganizer.setVisibility(User.ROLE_ORGANIZER.equals(currentRole) ? View.GONE : View.VISIBLE);
        mRoleChipAdmin.setVisibility(User.ROLE_ADMIN.equals(currentRole)        ? View.GONE : View.VISIBLE);

        resetRoleChips();
        mTvRoleChangeUserName.setText(user.getName() != null ? user.getName() : "");
        mOverlayRoleChange.setVisibility(View.VISIBLE);
    }

    private void dismissRoleChangeOverlay() {
        mPendingRoleUser = null;
        mSelectedRole    = null;
        mOverlayRoleChange.setVisibility(View.GONE);
    }

    private void resetRoleChips() {
        for (TextView chip : new TextView[]{mRoleChipStudent, mRoleChipOrganizer, mRoleChipAdmin}) {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(requireContext().getColor(R.color.color_text_secondary));
        }
    }

    private void selectRoleChip(TextView selected, String role) {
        resetRoleChips();
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(requireContext().getColor(R.color.white));
        mSelectedRole = role;
    }

    private void confirmRoleChange() {
        if (mSelectedRole == null) {
            showToast("Please select a role.");
            return;
        }
        if (mPendingRoleUser == null || mPendingRoleUser.getUid() == null
                || mPendingRoleUser.getUid().isEmpty()) {
            showToast("Error: could not identify user. Please reload the list.");
            dismissRoleChangeOverlay();
            return;
        }
        String newLabel = getLabelForRole(mSelectedRole);
        try {
            mAdminService.updateUserRole(
                    mPendingRoleUser.getUid(),
                    mSelectedRole,
                    () -> {
                        if (!isAdded()) return;
                        mPendingRoleUser.setRole(mSelectedRole);
                        mUserAdapter.updateUser(mPendingRoleUser);
                        showToast("Role updated to " + newLabel + ".");
                        dismissRoleChangeOverlay();
                    },
                    err -> {
                        if (!isAdded()) return;
                        showToast(err);
                    });
        } catch (Exception e) {
            showToast("Failed to update role: " + e.getMessage());
        }
    }

    private String getLabelForRole(String role) {
        if (User.ROLE_ADMIN.equals(role))     return "Admin";
        if (User.ROLE_ORGANIZER.equals(role)) return "Organizer";
        return "Student";
    }

    // ----------------------------------------------------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mRequestsListener != null) {
            mRequestsListener.remove();
            mRequestsListener = null;
        }
    }

    private void showToast(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
