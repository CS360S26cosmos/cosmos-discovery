package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.User;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.io.File;

public class EditProfile extends AppCompatActivity {

    private EditText etFullName, etBio;
    private AutoCompleteTextView etMajor;
    private Button btnSave;
    private ImageView ivProfileImage;
    private ProgressBar progressUpload;
    private TextView tvUploadStatus;

    private boolean mPhotoUploading;

    private ActivityResultLauncher<String> mPickPhotoLauncher;
    private ActivityResultLauncher<Intent> mCropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_edit_profile);

        etFullName     = findViewById(R.id.etFullName);
        etMajor        = findViewById(R.id.etMajor);
        etBio          = findViewById(R.id.etBio);
        btnSave        = findViewById(R.id.btnSave);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        progressUpload = findViewById(R.id.progressUpload);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);

        String[] majors = getResources().getStringArray(R.array.majors_array);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, majors);
        etMajor.setAdapter(adapter);

        User user = RoleUtil.getCurrentUser();
        if (user != null) {
            etFullName.setText(user.getName());
            etMajor.setText(user.getMajor(), false);
            etBio.setText(user.getBio());
            loadProfilePhoto(user.getPhotoUrl());
        }

        setupPhotoLaunchers();

        View.OnClickListener photoPicker = v -> mPickPhotoLauncher.launch("image/*");
        findViewById(R.id.profileImageContainer).setOnClickListener(photoPicker);
        findViewById(R.id.ivCameraOverlay).setOnClickListener(photoPicker);

        btnSave.setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void setupPhotoLaunchers() {
        mCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri croppedUri = UCrop.getOutput(result.getData());
                        if (croppedUri != null) {
                            Glide.with(this)
                                    .load(croppedUri)
                                    .centerCrop()
                                    .into(ivProfileImage);
                            uploadPhoto(croppedUri);
                        }
                    }
                }
        );

        mPickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) launchCrop(uri);
                }
        );
    }

    private void launchCrop(Uri sourceUri) {
        File destFile = new File(getCacheDir(), "profile_crop.jpg");
        Uri destUri = Uri.fromFile(destFile);

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setShowCropFrame(false);
        options.setToolbarColor(getResources().getColor(R.color.color_primary, getTheme()));
        options.setStatusBarColor(getResources().getColor(R.color.color_primary_dark, getTheme()));
        options.setCompressionQuality(80);

        Intent cropIntent = UCrop.of(sourceUri, destUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .getIntent(this);

        mCropLauncher.launch(cropIntent);
    }

    private void uploadPhoto(Uri croppedUri) {
        User user = RoleUtil.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            return;
        }

        mPhotoUploading = true;
        progressUpload.setVisibility(View.VISIBLE);
        tvUploadStatus.setVisibility(View.VISIBLE);
        tvUploadStatus.setText("Uploading photo…");

        String uid = user.getUid();
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("profile_photos/" + uid + "/profile.jpg");

        ref.putFile(croppedUri)
                .addOnProgressListener(snapshot -> {
                    long total = snapshot.getTotalByteCount();
                    if (total > 0) {
                        int pct = (int) (100 * snapshot.getBytesTransferred() / total);
                        progressUpload.setProgress(pct);
                        tvUploadStatus.setText("Uploading… " + pct + "%");
                    }
                })
                .addOnSuccessListener(task -> {
                    tvUploadStatus.setText("Saving…");
                    ref.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                String url = downloadUri.toString();
                                // Save photoUrl to Firestore immediately
                                FirebaseFirestore.getInstance()
                                        .collection("users").document(uid)
                                        .update("photoUrl", url)
                                        .addOnSuccessListener(unused -> {
                                            mPhotoUploading = false;
                                            progressUpload.setVisibility(View.GONE);
                                            tvUploadStatus.setText("Photo saved!");
                                            // Update in-memory user so sidebar/ViewProfile pick it up
                                            user.setPhotoUrl(url);
                                        })
                                        .addOnFailureListener(e -> {
                                            mPhotoUploading = false;
                                            progressUpload.setVisibility(View.GONE);
                                            tvUploadStatus.setText("Failed to save photo.");
                                        });
                            })
                            .addOnFailureListener(e -> {
                                mPhotoUploading = false;
                                progressUpload.setVisibility(View.GONE);
                                tvUploadStatus.setText("Could not get photo URL.");
                            });
                })
                .addOnFailureListener(e -> {
                    mPhotoUploading = false;
                    progressUpload.setVisibility(View.GONE);
                    tvUploadStatus.setText("Upload failed.");
                });
    }

    private void loadProfilePhoto(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_sidebar_main_profileimage)
                    .centerCrop()
                    .into(ivProfileImage);
        }
    }

    private void saveProfile() {
        if (mPhotoUploading) {
            Toast.makeText(this, "Photo is still uploading…", Toast.LENGTH_SHORT).show();
            return;
        }

        String name  = etFullName.getText().toString().trim();
        String major = etMajor.getText().toString().trim();
        String bio   = etBio.getText().toString().trim();

        if (name.isEmpty()) {
            etFullName.setError("Name required");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
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
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
                );
    }
}
