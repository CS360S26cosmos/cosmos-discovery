package com.example.cosmos_discovery.ui.shared;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.ui.student.DiscoverFragment;
import com.example.cosmos_discovery.util.RoleUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Only load a fragment on fresh launch, not on config change (rotation etc.)
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, resolveFragment())
                    .commit();
        }
    }

    /** Returns the correct root fragment based on the signed-in user's role. */
    private Fragment resolveFragment() {
        if (RoleUtil.isStudent()) {
            return new DiscoverFragment();
        }
        // Organizer and Admin fragments are not yet built — placeholder for now
        return new DiscoverFragment();
    }
}
