"""
Cosmos Discovery — RSVP seed script
=====================================
Populates attendeeIds on all approved events with realistic attendee counts,
and RSVPs the organizer account to a selection of events they didn't create
(covering both past and upcoming events).

Idempotent: fetches the current attendeeIds list, unions new UIDs in, and sets
rsvpCount = len(final list). Safe to re-run — never double-counts.

SETUP:
  pip install firebase-admin
  python scripts/seed_rsvps.py

Run AFTER seed_events.py and seed_users.py so events and user accounts exist.
"""

import os
import sys
import time
import random

try:
    import firebase_admin
    from firebase_admin import credentials, firestore, auth
except ImportError:
    print("Error: firebase-admin is not installed.")
    print("Run:  pip install firebase-admin")
    sys.exit(1)

# ── Firebase init ──────────────────────────────────────────────────────────────

KEY_PATH = os.path.join(
    os.path.dirname(__file__),
    "cosmos-discovery-firebase-adminsdk-fbsvc-1e9bae2712.json",
)

if not os.path.exists(KEY_PATH):
    print(f"Error: service account key not found at {KEY_PATH}")
    sys.exit(1)

if not firebase_admin._apps:
    cred = credentials.Certificate(KEY_PATH)
    firebase_admin.initialize_app(cred)

db = firestore.client()

# ── Config ────────────────────────────────────────────────────────────────────

ORGANIZER_EMAIL = "27100237@lums.edu.pk"

# All 12 seed student accounts (created by seed_users.py)
SEED_EMAILS = [
    "27100201@lums.edu.pk",
    "27100202@lums.edu.pk",
    "27100203@lums.edu.pk",
    "27100204@lums.edu.pk",
    "27100205@lums.edu.pk",
    "27100206@lums.edu.pk",
    "27100207@lums.edu.pk",
    "27100208@lums.edu.pk",
    "27100209@lums.edu.pk",
    "27100210@lums.edu.pk",
    "27100211@lums.edu.pk",
    "27100212@lums.edu.pk",
]

# Other real accounts (not the organizer)
OTHER_REAL_EMAILS = [
    "27100337@lums.edu.pk",  # sameen
    "27100026@lums.edu.pk",  # hamania
    "27100088@lums.edu.pk",  # ammara
    "27100052@lums.edu.pk",  # elizeh
]

# ── Helpers ────────────────────────────────────────────────────────────────────

def now_ms():
    return int(time.time() * 1000)


def get_uid_by_email(email):
    """Returns the Firebase Auth UID for an email, or None if not found."""
    try:
        return auth.get_user_by_email(email).uid
    except auth.UserNotFoundError:
        return None


def rsvp_uids_to_event(event_ref, uids_to_add):
    """
    Fetches the event's current attendeeIds, unions in uids_to_add,
    and writes back attendeeIds + rsvpCount atomically.
    Returns the final attendee count.
    """
    snap = event_ref.get()
    if not snap.exists:
        return 0
    data = snap.to_dict()
    current = set(data.get("attendeeIds") or [])
    final = list(current | set(uids_to_add))
    event_ref.update({"attendeeIds": final, "rsvpCount": len(final)})
    return len(final)


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    random.seed(7)  # reproducible

    # ── Step 1: Resolve UIDs ──────────────────────────────────────────────────
    print("🔍  Resolving user UIDs...")

    organizer_uid = get_uid_by_email(ORGANIZER_EMAIL)
    if organizer_uid is None:
        print(f"   Error: {ORGANIZER_EMAIL} not found in Firebase Auth. Run seed_users.py first.")
        sys.exit(1)
    print(f"   ✓  Organizer: {ORGANIZER_EMAIL} ({organizer_uid[:8]}...)")

    all_uids = []   # pool used for general attendee seeding (everyone)
    all_uids.append(organizer_uid)

    print("   Resolving seed students...")
    for email in SEED_EMAILS:
        uid = get_uid_by_email(email)
        if uid:
            all_uids.append(uid)
        else:
            print(f"   ⚠  {email} not found — skipping (run seed_users.py first)")

    print("   Resolving other real users...")
    for email in OTHER_REAL_EMAILS:
        uid = get_uid_by_email(email)
        if uid:
            all_uids.append(uid)
        else:
            print(f"   ⚠  {email} not found — skipping")

    print(f"   {len(all_uids)} UIDs resolved in total\n")

    # ── Step 2: Fetch all approved events ─────────────────────────────────────
    print("📅  Fetching approved events...")
    now = now_ms()
    events_snap = db.collection("events").where("status", "==", "approved").stream()

    past_not_owned     = []  # (doc_ref, title) — past events not by organizer
    upcoming_not_owned = []  # (doc_ref, title) — upcoming events not by organizer

    all_events = []  # (doc_ref, data) — all approved events

    for doc in events_snap:
        data = doc.to_dict()
        ref  = db.collection("events").document(doc.id)
        all_events.append((ref, data))

        is_owned = data.get("organizerId") == organizer_uid
        is_past  = data.get("dateTime", now) < now

        if not is_owned:
            if is_past:
                past_not_owned.append((ref, data.get("title", "?")))
            else:
                upcoming_not_owned.append((ref, data.get("title", "?")))

    print(f"   {len(all_events)} approved events found\n")

    # ── Step 3: Add attendees to all approved events ───────────────────────────
    print("🎟  Seeding attendees on approved events...")
    total_rsvps = 0

    for event_ref, data in all_events:
        title      = data.get("title", "?")
        capacity   = data.get("capacity", 0)
        is_owned   = data.get("organizerId") == organizer_uid

        # Candidates: everyone except the event's own organizer (they're the host)
        candidates = [uid for uid in all_uids
                      if uid != data.get("organizerId")]

        # Number to add: between 40–80% of capacity when capped, else 4–10 random
        if capacity > 0:
            max_add = max(3, int(capacity * 0.7))
            k = min(random.randint(int(capacity * 0.4), max_add), len(candidates))
        else:
            k = min(random.randint(4, 10), len(candidates))

        chosen = random.sample(candidates, k)
        final_count = rsvp_uids_to_event(event_ref, chosen)
        total_rsvps += len(chosen)
        owned_flag = " [your event]" if is_owned else ""
        print(f"   ✓  {title[:45]:<45}  {final_count} attendees{owned_flag}")

    # ── Step 4: RSVP the organizer to events they don't own ───────────────────
    print(f"\n🙋  RSVPing organizer to their own RSVPs (non-owned events)...")

    # Pick 2 past + 2 upcoming (or fewer if not enough events exist)
    random.shuffle(past_not_owned)
    random.shuffle(upcoming_not_owned)
    organizer_rsvp_events = past_not_owned[:2] + upcoming_not_owned[:2]

    if not organizer_rsvp_events:
        print("   ⚠  No non-owned approved events found — skipping organizer RSVPs")
    else:
        for event_ref, title in organizer_rsvp_events:
            rsvp_uids_to_event(event_ref, [organizer_uid])
            snap = event_ref.get()
            final_count = snap.to_dict().get("rsvpCount", "?")
            is_past = event_ref.get().to_dict().get("dateTime", now) < now
            label = "past" if is_past else "upcoming"
            print(f"   ✓  [{label}]  {title}")

    # ── Summary ───────────────────────────────────────────────────────────────
    print()
    print(f"🎉  Done!")
    print(f"   {len(all_events)} events updated")
    print(f"   ~{total_rsvps} attendee entries written")
    print(f"   Organizer RSVPed to {len(organizer_rsvp_events)} non-owned events")
    print()
    print("Open the app → Discover tab to see populated RSVP counts.")
    print("My Events tab (organizer) will show the events they personally RSVPed to.")


if __name__ == "__main__":
    main()
