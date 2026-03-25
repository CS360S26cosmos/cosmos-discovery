package com.example.cosmos_discovery.ui.student;

import android.os.Bundle;
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
import com.example.cosmos_discovery.adapter.EventSmallAdapter;
import com.example.cosmos_discovery.database.EventService;
import com.example.cosmos_discovery.util.RsvpHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

/**
 * Displays the scrollable list of upcoming approved events for the student role.
 * Hosted by {@link com.example.cosmos_discovery.ui.shared.MainActivity}.
 */
public class DiscoverFragment extends Fragment {

    private EventSmallAdapter    mAdapter;
    private final RsvpHandler    mRsvpHandler  = new RsvpHandler();
    private final EventService   mEventService = new EventService();
    private ListenerRegistration mEventsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        mAdapter = new EventSmallAdapter(requireContext(), new ArrayList<>(), (event, pos) ->
                mRsvpHandler.toggle(event,
                        () -> mAdapter.notifyItemChanged(pos),
                        err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Attach listener when fragment becomes visible
        mEventsListener = mEventService.listenUpcomingEvents(
                events -> mAdapter.updateData(events),
                err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detach listener when fragment leaves screen to stop unnecessary reads
        if (mEventsListener != null) {
            mEventsListener.remove();
            mEventsListener = null;
        }
    }
}
