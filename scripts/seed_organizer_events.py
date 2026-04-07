"""
Cosmos Discovery — Organizer Events seed script
================================================
Seeds the 'events' collection with test events for a specific organizer account,
creating a mix of pending, approved, and rejected events so the redesigned
"My Posted Events" page can be tested with realistic data.

Cleans up previously seeded events (keeps manually created ones like
"board game night") before seeding fresh.

SETUP:
  pip install firebase-admin
  python scripts/seed_organizer_events.py
"""

import os
import sys
import time
from datetime import datetime, timedelta

# ── Dependency check ──────────────────────────────────────────────────────────

try:
    import firebase_admin
    from firebase_admin import credentials, firestore, auth
except ImportError:
    print("Error: firebase-admin is not installed.")
    print("Run:  pip install firebase-admin")
    sys.exit(1)

# ── Firebase init ─────────────────────────────────────────────────────────────

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

# Titles of events to keep (not delete during cleanup)
KEEP_TITLES = {"board game night"}

# Titles of events this script creates (used to identify seeded events)
SEEDED_TITLES = {
    "Board Game Night",
    "Open Mic Poetry Night",
    "Midnight Coding Marathon",
    "Spring Photography Walk",
    "Tech Talk: Future of AI",
    "Campus Food Carnival",
    "Late Night Bonfire",
    "Off-Campus Concert Trip",
}

# ── Helpers ───────────────────────────────────────────────────────────────────

def now_ms():
    return int(time.time() * 1000)

def days(n):
    return int(n * 24 * 60 * 60 * 1000)

def hours(n):
    return int(n * 60 * 60 * 1000)

NOW = now_ms()

def at(day_offset, hour, minute=0):
    return NOW + days(day_offset) + hours(hour) + minute * 60 * 1000

def get_uid_by_email(email):
    try:
        return auth.get_user_by_email(email).uid
    except auth.UserNotFoundError:
        return None

# ── Event data ────────────────────────────────────────────────────────────────
# Matches the exact field structure used in seed_events.py (the working script).
# organizerId is set at runtime.

EVENTS = [
    # ── PENDING (3 events) ────────────────────────────────────────────────────
    {
        "title":       "Board Game Night",
        "dateTime":    at(5, 19),
        "location":    "LUMS Student Lounge",
        "tags":        ["Cultural", "Social"],
        "rsvpCount":   12,
        "imageUrl":    "https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?w=800",
        "status":      "pending",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Open Mic Poetry Night",
        "dateTime":    at(8, 18, 30),
        "location":    "PDC Lobby",
        "tags":        ["Arts", "Cultural"],
        "rsvpCount":   0,
        "imageUrl":    "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800",
        "status":      "pending",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Midnight Coding Marathon",
        "dateTime":    at(12, 22),
        "location":    "SBASSE Labs",
        "tags":        ["Technology", "Workshop"],
        "rsvpCount":   5,
        "imageUrl":    "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800",
        "status":      "pending",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },

    # ── APPROVED (3 events) ───────────────────────────────────────────────────
    {
        "title":       "Spring Photography Walk",
        "dateTime":    at(3, 16),
        "location":    "LUMS Main Lawn",
        "tags":        ["Arts", "Cultural"],
        "rsvpCount":   34,
        "imageUrl":    "https://images.unsplash.com/photo-1452587925148-ce544e77e70d?w=800",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Tech Talk: Future of AI",
        "dateTime":    at(6, 14),
        "location":    "SDSB Auditorium",
        "tags":        ["Technology", "Academic"],
        "rsvpCount":   78,
        "imageUrl":    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Campus Food Carnival",
        "dateTime":    at(10, 12),
        "location":    "REDC Lawn",
        "tags":        ["Food", "Cultural"],
        "rsvpCount":   156,
        "imageUrl":    "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },

    # ── REJECTED (2 events) ───────────────────────────────────────────────────
    {
        "title":       "Late Night Bonfire",
        "dateTime":    at(7, 23),
        "location":    "Cricket Ground",
        "tags":        ["Cultural"],
        "rsvpCount":   0,
        "imageUrl":    "https://images.unsplash.com/photo-1497906539264-eb74442e37a9?w=800",
        "status":      "rejected",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Off-Campus Concert Trip",
        "dateTime":    at(14, 20),
        "location":    "Liberty Roundabout, Lahore",
        "tags":        ["Music"],
        "rsvpCount":   0,
        "imageUrl":    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800",
        "status":      "rejected",
        "attendeeIds": [],
        "accessType":  "open",
    },
]

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    # Look up organizer UID
    print(f"Looking up organizer: {ORGANIZER_EMAIL}")
    organizer_uid = get_uid_by_email(ORGANIZER_EMAIL)
    if organizer_uid is None:
        print(f"Error: {ORGANIZER_EMAIL} not found in Firebase Auth.")
        sys.exit(1)
    print(f"Found UID: {organizer_uid}\n")

    # ── Cleanup: delete all events by this organizer except KEEP_TITLES ────────
    print("Cleaning up previous events...")
    events_ref = db.collection("events")
    existing = events_ref.where("organizerId", "==", organizer_uid).stream()
    deleted = 0
    kept = 0
    for doc in existing:
        title = doc.to_dict().get("title", "")
        if title.lower() in KEEP_TITLES:
            kept += 1
            print(f"  KEPT     {title}")
        else:
            doc.reference.delete()
            deleted += 1
            print(f"  DELETED  {title}")
    print(f"  {deleted} deleted, {kept} kept\n")

    # ── Seed events ───────────────────────────────────────────────────────────
    print("Seeding events...")
    col = db.collection("events")
    counts = {"pending": 0, "approved": 0, "rejected": 0}

    for event in EVENTS:
        doc = dict(event)
        doc["organizerId"] = organizer_uid
        doc["createdAt"] = now_ms()

        col.add(doc)
        counts[doc["status"]] += 1
        print(f"  [{doc['status']:8s}]  {doc['title']}")

    print()
    print(f"Seeded {len(EVENTS)} events for {ORGANIZER_EMAIL}:")
    print(f"  {counts['pending']} pending  |  {counts['approved']} approved  |  {counts['rejected']} rejected")
    print()
    print("Open the app, go to sidebar -> My Posted Events to verify.")


if __name__ == "__main__":
    main()
