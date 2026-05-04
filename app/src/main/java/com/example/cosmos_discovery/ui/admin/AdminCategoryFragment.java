package com.example.cosmos_discovery.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminCategoryFragment extends Fragment {

    private final EventService mEventService = new EventService();
    private ListenerRegistration mListener;

    private TextInputEditText mEtNew;
    private CategoryAdapter   mAdapter;
    private TextView          mTvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEtNew   = view.findViewById(R.id.etNewCategory);
        mTvEmpty = view.findViewById(R.id.tvEmptyCategories);

        RecyclerView rv = view.findViewById(R.id.rvCategories);
        mAdapter = new CategoryAdapter(this::confirmDelete);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(mAdapter);

        MaterialCardView btnAdd = view.findViewById(R.id.btnAddCategory);
        btnAdd.setOnClickListener(v -> handleAdd());

        mEtNew.setOnEditorActionListener((v, actionId, event) -> {
            handleAdd();
            return true;
        });

        attachListener();
    }

    private void attachListener() {
        mListener = mEventService.listenCategories(
                docs -> {
                    mAdapter.setData(docs);
                    mTvEmpty.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);
                },
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
        );
    }

    private void handleAdd() {
        String name = mEtNew.getText() != null
                ? mEtNew.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a category name.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (DocumentSnapshot doc : mAdapter.getDocs()) {
            String existing = doc.getString("name");
            if (existing != null && existing.equalsIgnoreCase(name)) {
                Toast.makeText(requireContext(), "Category already exists.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        mEventService.addCategory(name,
                () -> mEtNew.setText(""),
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
        );
    }

    private void confirmDelete(DocumentSnapshot doc) {
        String name = doc.getString("name");
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete category?")
                .setMessage("Delete \"" + name + "\"? Events already tagged will keep this "
                        + "category, but it won't appear in the picker for new events.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) ->
                        mEventService.deleteCategory(doc.getId(),
                                () -> {},
                                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                        )
                )
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mListener != null) {
            mListener.remove();
            mListener = null;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    interface DeleteListener {
        void onDelete(DocumentSnapshot doc);
    }

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

        private final DeleteListener         mDeleteListener;
        private       List<DocumentSnapshot> mDocs = new ArrayList<>();

        CategoryAdapter(DeleteListener listener) {
            mDeleteListener = listener;
        }

        void setData(List<DocumentSnapshot> docs) {
            mDocs = docs;
            notifyDataSetChanged();
        }

        List<DocumentSnapshot> getDocs() {
            return mDocs;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DocumentSnapshot doc = mDocs.get(position);
            String name = doc.getString("name");
            h.tvName.setText(name != null ? name : "");
            h.btnDelete.setOnClickListener(v -> mDeleteListener.onDelete(doc));
        }

        @Override
        public int getItemCount() {
            return mDocs.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvName;
            final View     btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName    = itemView.findViewById(R.id.tvCategoryName);
                btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
            }
        }
    }
}
