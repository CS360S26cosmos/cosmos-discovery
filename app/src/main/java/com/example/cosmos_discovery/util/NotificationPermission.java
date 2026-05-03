package com.example.cosmos_discovery.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Asks for the Android 13+ POST_NOTIFICATIONS runtime permission once.
 * No-op on older platforms (notifications were granted at install time).
 */
public final class NotificationPermission {

    public static final int REQUEST_CODE = 4711;

    private NotificationPermission() {}

    public static void requestIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        ActivityCompat.requestPermissions(
                activity,
                new String[] { Manifest.permission.POST_NOTIFICATIONS },
                REQUEST_CODE);
    }
}
