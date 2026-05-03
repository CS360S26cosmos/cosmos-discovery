/**
 * Cloud Functions for cosmos-discovery.
 *
 * onNotificationCreated: triggered when a Notification doc is written to
 * users/{uid}/notifications/{notifId}. Reads the recipient's fcmToken and
 * notifPrefs from their user doc, then sends an FCM push if allowed.
 *
 * Client code (RsvpHandler, FriendService, EditEventActivity, AnnouncementService,
 * AdminEventQueueFragment, etc.) keeps writing to the notifications subcollection
 * exactly as it does today — this function is the single FCM gateway.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();

// Notification.type → notifPrefs key. Missing entries fall through to the
// master "push" check only.
const TYPE_TO_PREF = {
  rsvp_confirmed:           "rsvp",
  event_updated:            "eventUpdates",
  event_cancelled:          "eventUpdates",
  announcement:             "announcements",
  friend_request_received:  "friendRequests",
  friend_request_accepted:  "friendRequests",
  event_approved:           "adminDecisions",
  event_rejected:           "adminDecisions",
  capacity_full:            "capacityFull",
  rsvp_received:            "rsvpReceived",
};

exports.onNotificationCreated = onDocumentCreated(
  "users/{uid}/notifications/{notifId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const notif = snap.data() || {};
    const { uid, notifId } = event.params;

    const userSnap = await admin.firestore().doc(`users/${uid}`).get();
    if (!userSnap.exists) {
      logger.info("Skip: recipient user doc missing", { uid });
      return;
    }
    const user = userSnap.data() || {};
    const token = user.fcmToken;
    if (!token) {
      logger.info("Skip: no fcmToken on recipient", { uid });
      return;
    }

    const prefs = user.notifPrefs || {};
    if (prefs.push === false) {
      logger.info("Skip: master push disabled", { uid });
      return;
    }
    const prefKey = TYPE_TO_PREF[notif.type];
    if (prefKey && prefs[prefKey] === false) {
      logger.info("Skip: per-category disabled", { uid, type: notif.type });
      return;
    }

    const data = {
      type:    notif.type    || "",
      eventId: notif.eventId || "",
      notifId,
    };

    try {
      await admin.messaging().send({
        token,
        notification: {
          title: notif.title   || "Cosmos Discovery",
          body:  notif.message || "",
        },
        data,
        android: {
          priority: "high",
          notification: {
            channelId: "cosmos_default",
          },
        },
      });
    } catch (err) {
      // Stale token — clean it up so we stop trying next time.
      if (
        err.code === "messaging/registration-token-not-registered" ||
        err.code === "messaging/invalid-registration-token"
      ) {
        await admin.firestore().doc(`users/${uid}`).update({
          fcmToken: admin.firestore.FieldValue.delete(),
        });
        logger.info("Cleared stale FCM token", { uid });
      } else {
        logger.error("FCM send failed", { uid, err });
      }
    }
  }
);
