package com.example.cosmos_discovery.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.adapter.FriendListAdapter;
import com.example.cosmos_discovery.database.FriendService;

import java.util.ArrayList;

/**
 * Full-screen list of a user's friends.
 *
 * Launched with:
 *   {@link #EXTRA_USER_ID} — UID whose friends to display
 *   {@link #EXTRA_TITLE}   — optional screen title (defaults to "Friends")
 *
 * Tapping a friend opens {@link UserProfileActivity} for that friend.
 */
public class FriendsListActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String EXTRA_TITLE   = "EXTRA_TITLE";

    private final FriendService mFriendService = new FriendService();

    private FriendListAdapter mAdapter;
    private TextView          mTvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String title  = getIntent().getStringExtra(EXTRA_TITLE);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(title != null ? title : "Friends");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvFriends);
        mTvEmpty = findViewById(R.id.tvEmpty);

        mAdapter = new FriendListAdapter(new ArrayList<>(), entry -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_USER_ID, entry.getUid());
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mAdapter);

        if (userId != null) {
            mFriendService.fetchFriends(userId,
                    entries -> {
                        mAdapter.updateData(entries);
                        mTvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
                    },
                    err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
        }
    }
}
