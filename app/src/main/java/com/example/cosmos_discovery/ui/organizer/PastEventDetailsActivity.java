package com.example.cosmos_discovery.ui.organizer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.database.EventService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.cosmos_discovery.model.Event;

public class PastEventDetailsActivity extends AppCompatActivity {

    private final EventService mEventService = new EventService();

    private String mEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pastevent_details_page);

        mEventId = getIntent().getStringExtra("event_id");

        if (mEventId == null || mEventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        fetchAndBind();
    }

    private void fetchAndBind() {
        mEventService.fetchEventById(
                mEventId,
                this::bindEvent,
                err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        );
    }
    private void bindEvent(Event event) {
        ImageView image = findViewById(R.id.imageViewEvent);
        TextView title = findViewById(R.id.textViewEventTitle);
        TextView venue = findViewById(R.id.tvVenueValue);
        TextView dateTime = findViewById(R.id.tvDateTimeValue);
        TextView organizer = findViewById(R.id.tvOrganizerValue);
        TextView desc = findViewById(R.id.tvDescriptionValue);

        Glide.with(this)
                .load(event.getImageUrl())
                .placeholder(R.color.color_text_hint)
                .centerCrop()
                .into(image);

        title.setText(event.getTitle());

        venue.setText(event.getLocation());

        SimpleDateFormat sdf =
                new SimpleDateFormat("MMM d, yyyy | h:mma", Locale.getDefault());

        dateTime.setText(sdf.format(new Date(event.getDateTime())));

        organizer.setText("Organizer");

        desc.setText(
                event.getDescription() != null &&
                        !event.getDescription().trim().isEmpty()
                        ? event.getDescription()
                        : "No description provided."
        );
    }
}