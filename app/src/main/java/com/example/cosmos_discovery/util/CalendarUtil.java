package com.example.cosmos_discovery.util;

import android.content.Intent;
import android.provider.CalendarContract;

import com.example.cosmos_discovery.model.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalendarUtil {

    public static Intent buildCalendarIntent(Event event) {
        long startMs = event.getDateTime();
        long endMs = parseEndTime(event.getDateOfEvent(), event.getEndTime(), startMs);

        return new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.getTitle())
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation())
                .putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                .putExtra(CalendarContract.Events.ALL_DAY, false);
    }

    static long parseEndTime(String dateOfEvent, String endTime, long startMs) {
        if (dateOfEvent == null || endTime == null || endTime.trim().isEmpty()) {
            return startMs + 3_600_000L;
        }
        try {
            String combined = dateOfEvent.trim() + " " + endTime.trim().toUpperCase(Locale.US);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mma", Locale.US);
            Date parsed = sdf.parse(combined);
            if (parsed == null) {
                return startMs + 3_600_000L;
            }
            long endMs = parsed.getTime();
            if (endMs < startMs) {
                endMs += 86_400_000L;
            }
            return endMs;
        } catch (ParseException e) {
            return startMs + 3_600_000L;
        }
    }
}
