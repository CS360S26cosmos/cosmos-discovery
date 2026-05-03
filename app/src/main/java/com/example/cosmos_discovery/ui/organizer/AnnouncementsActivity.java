package com.example.cosmos_discovery.ui.organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.AnnouncementAdapter;
import com.example.cosmos_discovery.database.AnnouncementService;
import com.example.cosmos_discovery.model.Announcement;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Per-event announcements screen for organizers.
 * Shows the announcement history (newest-first) and lets the organizer compose a new
 * one (300-char cap). Send is disabled when the message is empty/over-limit/uploading.
 */
public class AnnouncementsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID    = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";

    private final AnnouncementService mService = new AnnouncementService();

    private String               mEventId;
    private AnnouncementAdapter  mAdapter;
    private ListenerRegistration mListener;
    private Button               mBtnSend;
    private boolean              mSending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        if (!RoleUtil.isOrganizer()) {
            Toast.makeText(this, "Only organizers can send announcements.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        ((ImageView) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());

        TextView tvLabel = findViewById(R.id.tvEventLabel);
        if (eventTitle != null && !eventTitle.isEmpty()) {
            tvLabel.setText("For: " + eventTitle);
            tvLabel.setVisibility(View.VISIBLE);
        } else {
            tvLabel.setVisibility(View.GONE);
        }

        RecyclerView rv = findViewById(R.id.rvAnnouncements);
        TextView empty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new AnnouncementAdapter();
        rv.setAdapter(mAdapter);

        EditText  et       = findViewById(R.id.etMessage);
        TextView  counter  = findViewById(R.id.tvCounter);
        mBtnSend           = findViewById(R.id.btnSend);

        mBtnSend.setEnabled(false);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                int len = s.length();
                counter.setText(len + " / " + Announcement.MAX_LENGTH);
                mBtnSend.setEnabled(!mSending && len > 0 && len <= Announcement.MAX_LENGTH);
            }
        });

        mBtnSend.setOnClickListener(v -> {
            String text = et.getText().toString().trim();
            if (text.isEmpty()) return;
            mSending = true;
            mBtnSend.setEnabled(false);
            String organizerUid = RoleUtil.getCurrentUser() != null
                    ? RoleUtil.getCurrentUser().getUid() : null;
            mService.sendAnnouncement(mEventId, text, organizerUid,
                    () -> {
                        mSending = false;
                        et.setText("");
                        Toast.makeText(this, "Announcement sent.", Toast.LENGTH_SHORT).show();
                    },
                    err -> {
                        mSending = false;
                        mBtnSend.setEnabled(et.getText().length() > 0);
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
        });

        mListener = mService.listenAnnouncements(mEventId,
                list -> {
                    mAdapter.updateData(list);
                    empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                },
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null) {
            mListener.remove();
            mListener = null;
        }
    }
}
