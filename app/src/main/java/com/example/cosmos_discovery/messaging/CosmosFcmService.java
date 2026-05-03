package com.example.cosmos_discovery.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.cosmos_discovery.R;
import com.example.cosmos_discovery.model.Notification;
import com.example.cosmos_discovery.ui.notifications.NotificationsActivity;
import com.example.cosmos_discovery.ui.organizer.EventDetailsActivity;
import com.example.cosmos_discovery.ui.student.FriendRequestsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives FCM messages and surfaces them as Android system notifications.
 * Token refreshes are written back to the signed-in user's Firestore doc.
 *
 * Background-delivered messages with a {@code notification} payload are auto-displayed by
 * the FCM SDK using the manifest meta-data; this class handles foreground delivery and
 * data-only payloads explicitly.
 */
public class CosmosFcmService extends FirebaseMessagingService {

    private static final String TAG = "CosmosFcmService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to persist FCM token", e));
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();
        String type    = data.get("type");
        String eventId = data.get("eventId");

        String title = null;
        String body  = null;
        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body  = message.getNotification().getBody();
        }
        if (title == null) title = data.get("title");
        if (body  == null) body  = data.get("body");
        if (title == null && body == null) return;

        showSystemNotification(title, body, type, eventId);
    }

    private void showSystemNotification(String title, String body, String type, String eventId) {
        ensureChannel();

        Intent target = routeFor(type, eventId);
        target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), target, flags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this,
                getString(R.string.fcm_default_channel_id))
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title != null ? title : getString(R.string.app_name))
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(this)
                    .notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted; nothing to do.
        }
    }

    private Intent routeFor(String type, String eventId) {
        if (type == null) return new Intent(this, NotificationsActivity.class);

        switch (type) {
            case Notification.TYPE_RSVP_CONFIRMED:
            case Notification.TYPE_EVENT_UPDATED:
            case Notification.TYPE_EVENT_CANCELLED:
            case Notification.TYPE_ANNOUNCEMENT:
            case Notification.TYPE_EVENT_APPROVED:
            case Notification.TYPE_EVENT_REJECTED:
            case Notification.TYPE_CAPACITY_FULL:
                if (eventId != null && !eventId.isEmpty()) {
                    Intent i = new Intent(this, EventDetailsActivity.class);
                    i.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, eventId);
                    return i;
                }
                break;
            case Notification.TYPE_FRIEND_REQUEST_RECEIVED:
            case Notification.TYPE_FRIEND_REQUEST_ACCEPTED:
                return new Intent(this, FriendRequestsActivity.class);
        }
        return new Intent(this, NotificationsActivity.class);
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        String id = getString(R.string.fcm_default_channel_id);
        if (nm.getNotificationChannel(id) != null) return;
        NotificationChannel ch = new NotificationChannel(
                id, getString(R.string.fcm_default_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(ch);
    }
}
