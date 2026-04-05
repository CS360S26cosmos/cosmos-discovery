package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

public class EditProfile extends AppCompatActivity {

    private EditText etFullName, etBio;
    private AutoCompleteTextView etMajor;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_edit_profile); // reuse your UI

        etFullName = findViewById(R.id.etFullName);
        etMajor    = findViewById(R.id.etMajor);
        etBio      = findViewById(R.id.etBio);
        btnSave    = findViewById(R.id.btnSave);

        String[] majors = getResources().getStringArray(R.array.majors_array);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, majors);

        etMajor.setAdapter(adapter);

        // Get current user
        User user = RoleUtil.getCurrentUser();

        // Populate fields
        if (user != null) {
            etFullName.setText(user.getName());
            etMajor.setText(user.getMajor(), false);
            etBio.setText(user.getBio());
        }

        btnSave.setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void saveProfile() {
        String name  = etFullName.getText().toString().trim();
        String major = etMajor.getText().toString().trim();
        String bio   = etBio.getText().toString().trim();

        if (name.isEmpty()) {
            etFullName.setError("Name required");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(
                        "name", name,
                        "major", major,
                        "bio", bio
                )
                .addOnSuccessListener(unused -> {
                    User user = RoleUtil.getCurrentUser();
                    if (user != null) {
                        user.setName(name);
                        user.setMajor(major);
                        user.setBio(bio);
                    }
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                });
    }
}