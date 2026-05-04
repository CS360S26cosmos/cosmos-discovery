"""
Cosmos Discovery — RSVP & stats seed script
============================================
Populates each approved event with realistic RSVP, check-in, and per-RSVP
timestamp data so admin and event-stats screens have meaningful charts.

For every approved event this script writes:
  - attendeeIds  : list of UIDs (real seed users + synthetic UIDs to simulate
                   a larger campus population so capacity bars vary)
  - rsvpCount    : len(attendeeIds)
  - checkedInIds : 60–85% of attendees on past events (empty for upcoming)
  - subcollection events/{id}/rsvps/{uid} with {uid, timestamp} — timestamps
    are spread between createdAt and the event date using a front-loaded +
    deadline-rush distribution that mimics real RSVP behaviour.

Per-event attendee count is drawn from one of three tiers (high / moderate /
low demand) so the admin top-7 chart and per-event capacity bars look varied
rather than uniformly half-full.

Re-runs are idempotent: the script wipes the rsvps subcollection and rewrites
attendeeIds from scratch each run.

SETUP:
  pip install firebase-admin
  python scripts/seed_rsvps.py

Run AFTER seed_events.py and seed_users.py.
"""

import os
import random
import sys
import time
import uuid

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
    "27100201@lums.edu.pk", "27100202@lums.edu.pk", "27100203@lums.edu.pk",
    "27100204@lums.edu.pk", "27100205@lums.edu.pk", "27100206@lums.edu.pk",
    "27100207@lums.edu.pk", "27100208@lums.edu.pk", "27100209@lums.edu.pk",
    "27100210@lums.edu.pk", "27100211@lums.edu.pk", "27100212@lums.edu.pk",
]

# Other real accounts (not the organizer)
OTHER_REAL_EMAILS = [
    "27100337@lums.edu.pk", "27100026@lums.edu.pk",
    "27100088@lums.edu.pk", "27100052@lums.edu.pk",
]

# Per-event demand tier weights (chosen at random per event for variety)
TIER_WEIGHTS = ["high", "high", "moderate", "moderate", "moderate", "low"]

# Floor / ceiling for uncapped events
UNCAPPED_RANGE = (15, 60)

# Hard ceiling so we don't write tens of thousands of subcollection docs
MAX_ATTENDEES = 250


# ── Helpers ────────────────────────────────────────────────────────────────────

def now_ms():
    return int(time.time() * 1000)


def get_uid_by_email(email):
    try:
        return auth.get_user_by_email(email).uid
    except auth.UserNotFoundError:
        return None


def attendee_target(capacity, tier):
    """Pick a target attendee count for an event given its capacity and tier."""
    if capacity == 0:
        lo, hi = UNCAPPED_RANGE
        return random.randint(lo, hi)
    if tier == "high":
        return random.randint(int(capacity * 0.80), capacity)
    if tier == "moderate":
        return random.randint(int(capacity * 0.40), int(capacity * 0.65))
    # low
    return random.randint(max(1, int(capacity * 0.10)), max(2, int(capacity * 0.25)))


def gen_attendees(real_pool, target_count):
    """
    Returns a list of UIDs of length min(target_count, MAX_ATTENDEES). Real users
    fill the list first; remaining slots are filled with synthetic UIDs so
    capacity-bound events can look realistically full even though the real-user
    pool is small.
    """
    target_count = min(target_count, MAX_ATTENDEES)
    n_real = min(target_count, len(real_pool))
    chosen_real = random.sample(real_pool, n_real)
    n_synth = target_count - n_real
    synth = [f"seed_attendee_{uuid.uuid4().hex[:14]}" for _ in range(n_synth)]
    return chosen_real + synth


def realistic_rsvp_timestamp(start_ms, end_ms):
    """
    Returns a timestamp in [start_ms, end_ms] using a front-loaded + deadline-rush
    distribution: ~35% of RSVPs in the first 20% of the window (announcement
    burst), ~30% in the last 20% (deadline rush), ~35% spread across the middle.
    """
    span = max(1, end_ms - start_ms)
    r = random.random()
    if r < 0.35:
        return int(start_ms + random.uniform(0.00, 0.20) * span)
    if r < 0.65:
        return int(start_ms + random.uniform(0.20, 0.80) * span)
    return int(start_ms + random.uniform(0.80, 1.00) * span)


def delete_rsvps_subcollection(event_ref):
    """Wipes events/{id}/rsvps so a re-run starts clean."""
    rsvps = event_ref.collection("rsvps")
    while True:
        docs = list(rsvps.limit(500).stream())
        if not docs:
            return
        batch = db.batch()
        for d in docs:
            batch.delete(d.reference)
        batch.commit()


def write_rsvps(event_ref, attendees, start_ms, end_ms):
    """Writes one rsvps subcollection doc per attendee with a realistic timestamp."""
    batch = db.batch()
    written = 0
    in_batch = 0
    for uid in attendees:
        ts = realistic_rsvp_timestamp(start_ms, end_ms)
        batch.set(
            event_ref.collection("rsvps").document(uid),
            {"uid": uid, "timestamp": ts},
        )
        in_batch += 1
        if in_batch == 450:  # well under the 500-op batch limit
            batch.commit()
            written += in_batch
            in_batch = 0
            batch = db.batch()
    if in_batch > 0:
        batch.commit()
        written += in_batch
    return written


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    random.seed(7)

    # ── Step 1: Resolve UIDs ──────────────────────────────────────────────────
    print("🔍  Resolving user UIDs...")
    organizer_uid = get_uid_by_email(ORGANIZER_EMAIL)
    if organizer_uid is None:
        print(f"   Error: {ORGANIZER_EMAIL} not found in Firebase Auth. Run seed_users.py first.")
        sys.exit(1)
    print(f"   ✓  Organizer: {ORGANIZER_EMAIL} ({organizer_uid[:8]}...)")

    real_pool = [organizer_uid]
    for email in SEED_EMAILS + OTHER_REAL_EMAILS:
        uid = get_uid_by_email(email)
        if uid:
            real_pool.append(uid)
        else:
            print(f"   ⚠  {email} not found — skipping")
    print(f"   {len(real_pool)} real UIDs in candidate pool\n")

    # ── Step 2: Fetch all approved events ─────────────────────────────────────
    print("📅  Fetching approved events...")
    now = now_ms()
    events_snap = list(db.collection("events").where("status", "==", "approved").stream())
    print(f"   {len(events_snap)} approved events found\n")

    # ── Step 3: For each event: pick tier, build attendee list, write check-ins
    #          + timestamp subcollection ──────────────────────────────────────
    print("🎟  Seeding RSVPs, check-ins, and timestamp subcollection...")
    total_attendees = 0
    total_checkins  = 0
    total_timestamps = 0
    past_not_owned     = []
    upcoming_not_owned = []

    for doc in events_snap:
        data       = doc.to_dict()
        event_ref  = db.collection("events").document(doc.id)
        title      = (data.get("title") or "?")[:45]
        capacity   = int(data.get("capacity") or 0)
        created_at = int(data.get("createdAt") or now)
        date_time  = int(data.get("dateTime") or now)
        organizer  = data.get("organizerId")
        is_past    = date_time < now
        is_owned   = organizer == organizer_uid

        # Real candidates exclude the host (the organizer doesn't RSVP to their
        # own event in this seeding pass — we add them to specific non-owned
        # events below).
        host_excluded_pool = [u for u in real_pool if u != organizer]

        tier   = random.choice(TIER_WEIGHTS)
        target = attendee_target(capacity, tier)

        attendees = gen_attendees(host_excluded_pool, target)

        # ── Wipe + rewrite attendeeIds ──
        delete_rsvps_subcollection(event_ref)
        event_ref.update({
            "attendeeIds": attendees,
            "rsvpCount":   len(attendees),
        })

        # ── Timestamps subcollection: window = [createdAt, min(now, dateTime)] ──
        end_window = min(now, date_time)
        if end_window <= created_at:
            end_window = created_at + 1
        written = write_rsvps(event_ref, attendees, created_at, end_window)

        # ── Check-ins: only for past events ──
        checkin_count = 0
        if is_past and attendees:
            ratio = random.uniform(0.60, 0.85)
            checkin_count = max(1, int(len(attendees) * ratio))
            checked = random.sample(attendees, checkin_count)
            event_ref.update({"checkedInIds": checked})
        else:
            event_ref.update({"checkedInIds": []})

        if not is_owned:
            (past_not_owned if is_past else upcoming_not_owned).append((event_ref, title))

        total_attendees  += len(attendees)
        total_checkins   += checkin_count
        total_timestamps += written
        cap_label = f"{capacity}" if capacity else "∞"
        print(f"   ✓  [{tier:8s}]  {title:<45}  RSVPs={len(attendees):3d}/{cap_label:>3s}  "
              f"checkins={checkin_count:3d}  ts={written:3d}")

    # ── Step 4: Make sure the organizer has 2 past + 2 upcoming RSVPs of their
    #          own (so their My Events tab is populated) ─────────────────────
    print(f"\n🙋  RSVPing organizer to a few non-owned events...")
    random.shuffle(past_not_owned)
    random.shuffle(upcoming_not_owned)
    organizer_extra = past_not_owned[:2] + upcoming_not_owned[:2]
    for event_ref, title in organizer_extra:
        snap = event_ref.get()
        data = snap.to_dict() or {}
        ids = list(data.get("attendeeIds") or [])
        if organizer_uid in ids:
            print(f"   ✓  (already in)  {title}")
            continue
        ids.append(organizer_uid)
        event_ref.update({"attendeeIds": ids, "rsvpCount": len(ids)})
        # Add to subcollection — pick a recent realistic timestamp.
        created_at = int(data.get("createdAt") or now)
        date_time  = int(data.get("dateTime") or now)
        ts = realistic_rsvp_timestamp(created_at, min(now, date_time))
        event_ref.collection("rsvps").document(organizer_uid).set(
            {"uid": organizer_uid, "timestamp": ts}
        )
        # If past, also check the organizer in to roughly half of those.
        if date_time < now and random.random() < 0.5:
            checked = list(data.get("checkedInIds") or [])
            checked.append(organizer_uid)
            event_ref.update({"checkedInIds": checked})
        print(f"   ✓  {title}")

    # ── Summary ───────────────────────────────────────────────────────────────
    print()
    print("🎉  Done!")
    print(f"   {len(events_snap)} events updated")
    print(f"   {total_attendees} total attendee entries")
    print(f"   {total_checkins} total check-ins (past events only)")
    print(f"   {total_timestamps} timestamp docs in events/*/rsvps")
    print(f"   organizer RSVPed to {len(organizer_extra)} non-owned events")
    print()
    print("Open the app:")
    print("  • Discover           → varied RSVP counts on event cards")
    print("  • Admin dashboard    → top-7 chart shows variance + recent-events count")
    print("  • Event stats        → capacity bar varies, RSVP-vs-checkin bars meaningful,")
    print("                          RSVPs-per-day timeline scrubs back to event creation")


if __name__ == "__main__":
    main()
