"""
Cosmos Discovery — Per-Organizer RSVP seed script
==================================================
Walks every real organizer account (users.role == "organizer") and guarantees
that all of their approved events have realistic RSVP, check-in, and per-RSVP
timestamp data. Use this when you've added new organizer accounts and want
their event-stats screens to look populated, or to top up data after creating
new events as an organizer.

For each organizer's approved events the script writes:
  - attendeeIds  : tier-based realistic count (high / moderate / low demand),
                   padded with synthetic UIDs when the real-user pool is small
  - rsvpCount    : len(attendeeIds)
  - checkedInIds : 60–85% of attendees on past events; empty on upcoming
  - subcollection events/{id}/rsvps/{uid} with {uid, timestamp} drawn from a
                   front-loaded + deadline-rush distribution

Per-organizer reporting confirms no organizer was missed and shows event
totals so you can spot organizers whose dashboards will look empty.

Re-runs are idempotent: clears events/{id}/rsvps and overwrites attendeeIds.

SETUP:
  pip install firebase-admin
  python scripts/seed_organizer_rsvps.py

Run AFTER seed_events.py, seed_users.py, and seed_organizer_events.py.
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

# Per-event demand tier weights (chosen at random per event for variety)
TIER_WEIGHTS = ["high", "high", "moderate", "moderate", "moderate", "low"]

# Floor / ceiling for uncapped events
UNCAPPED_RANGE = (15, 60)

# Hard ceiling so we don't write tens of thousands of subcollection docs
MAX_ATTENDEES = 250


# ── Helpers ────────────────────────────────────────────────────────────────────

def now_ms():
    return int(time.time() * 1000)


def fetch_real_pool(exclude_organizer_uid):
    """
    Returns a list of real user UIDs (any role) excluding the host organizer.
    These are used to populate the front of attendeeIds before synthetic
    padding kicks in. Anyone in users/ counts — students, organizers, admins —
    so larger campuses give more real-user RSVPs and fewer synthetic ones.
    """
    pool = []
    for doc in db.collection("users").stream():
        uid = doc.id
        if uid != exclude_organizer_uid:
            pool.append(uid)
    return pool


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
    capacity-bound events can look realistically full even when the real-user
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
    distribution: ~35% in the first 20% of the window (announcement burst),
    ~30% in the last 20% (deadline rush), ~35% spread across the middle.
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
        if in_batch == 450:
            batch.commit()
            written += in_batch
            in_batch = 0
            batch = db.batch()
    if in_batch > 0:
        batch.commit()
        written += in_batch
    return written


def seed_one_event(event_ref, data, real_pool, now):
    """
    Populates a single event's RSVPs / check-ins / timestamp subcollection.
    Returns (attendees, checkins, timestamps_written) for reporting.
    """
    capacity   = int(data.get("capacity") or 0)
    created_at = int(data.get("createdAt") or now)
    date_time  = int(data.get("dateTime") or now)
    is_past    = date_time < now

    tier   = random.choice(TIER_WEIGHTS)
    target = attendee_target(capacity, tier)
    attendees = gen_attendees(real_pool, target)

    delete_rsvps_subcollection(event_ref)
    event_ref.update({
        "attendeeIds": attendees,
        "rsvpCount":   len(attendees),
    })

    end_window = min(now, date_time)
    if end_window <= created_at:
        end_window = created_at + 1
    written = write_rsvps(event_ref, attendees, created_at, end_window)

    checkin_count = 0
    if is_past and attendees:
        ratio = random.uniform(0.60, 0.85)
        checkin_count = max(1, int(len(attendees) * ratio))
        checked = random.sample(attendees, checkin_count)
        event_ref.update({"checkedInIds": checked})
    else:
        event_ref.update({"checkedInIds": []})

    return len(attendees), checkin_count, written, tier


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    random.seed(19)
    now = now_ms()

    # ── Step 1: Fetch every organizer account ─────────────────────────────────
    print("👥  Finding organizer accounts...")
    organizers = []
    for doc in db.collection("users").where("role", "==", "organizer").stream():
        u = doc.to_dict() or {}
        organizers.append({
            "uid":   doc.id,
            "name":  u.get("name") or "(unnamed)",
            "email": u.get("email") or "",
        })

    if not organizers:
        print("   ⚠  No organizer accounts found — nothing to seed.")
        print("   Hint: make sure at least one user has role == \"organizer\" in users/.")
        return

    print(f"   Found {len(organizers)} organizer account(s):")
    for org in organizers:
        print(f"     • {org['name']:<30s}  {org['email']}")
    print()

    # ── Step 2: For each organizer, seed RSVPs on their approved events ───────
    grand_events     = 0
    grand_attendees  = 0
    grand_checkins   = 0
    grand_timestamps = 0
    organizers_with_no_events = []

    for org in organizers:
        print(f"📂  {org['name']}  ({org['email']})")
        # Real pool: all real users except this organizer (they don't RSVP to
        # their own event in this seeding pass).
        real_pool = fetch_real_pool(exclude_organizer_uid=org["uid"])

        # Pull approved events owned by this organizer.
        events = list(
            db.collection("events")
              .where("organizerId", "==", org["uid"])
              .where("status", "==", "approved")
              .stream()
        )

        if not events:
            print(f"   ⚠  No approved events — skipping (pending/rejected events legitimately have 0 RSVPs)")
            organizers_with_no_events.append(org)
            print()
            continue

        per_org_attendees  = 0
        per_org_checkins   = 0
        per_org_timestamps = 0

        for doc in events:
            data      = doc.to_dict() or {}
            event_ref = db.collection("events").document(doc.id)
            title     = (data.get("title") or "?")[:42]
            capacity  = int(data.get("capacity") or 0)

            n_att, n_chk, n_ts, tier = seed_one_event(event_ref, data, real_pool, now)
            per_org_attendees  += n_att
            per_org_checkins   += n_chk
            per_org_timestamps += n_ts
            cap_label = f"{capacity}" if capacity else "∞"
            print(f"   ✓  [{tier:8s}]  {title:<42}  RSVPs={n_att:3d}/{cap_label:>3s}  "
                  f"checkins={n_chk:3d}  ts={n_ts:3d}")

        grand_events     += len(events)
        grand_attendees  += per_org_attendees
        grand_checkins   += per_org_checkins
        grand_timestamps += per_org_timestamps
        print(f"   → {len(events)} events, {per_org_attendees} RSVPs, "
              f"{per_org_checkins} check-ins, {per_org_timestamps} timestamps\n")

    # ── Summary ───────────────────────────────────────────────────────────────
    print("🎉  Done!")
    print(f"   {len(organizers)} organizers walked")
    print(f"   {grand_events} approved events seeded")
    print(f"   {grand_attendees} total attendee entries")
    print(f"   {grand_checkins} total check-ins (past events only)")
    print(f"   {grand_timestamps} timestamp docs in events/*/rsvps")

    if organizers_with_no_events:
        print()
        print(f"⚠  {len(organizers_with_no_events)} organizer(s) had no approved events:")
        for org in organizers_with_no_events:
            print(f"     • {org['name']}  ({org['email']})")
        print("   Their event-stats screens will be empty until they create approved events.")

    print()
    print("Open the app:")
    print("  • Sign in as any organizer → My Posted Events → tap any approved event")
    print("    → Stats: capacity bar varies, RSVP-vs-checkin gap is visible,")
    print("      RSVPs-per-day timeline scrubs back to event creation week.")


if __name__ == "__main__":
    main()
