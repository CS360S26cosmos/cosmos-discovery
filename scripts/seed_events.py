"""
Cosmos Discovery — Firestore seed script
========================================
Clears the 'events' and 'categories' collections, then populates them with
realistic test data that covers every field in the Event model.

SETUP:
  1. pip install firebase-admin
  2. Firebase Console → Project Settings → Service Accounts
       → Generate New Private Key → save as scripts/serviceAccountKey.json
  3. python scripts/seed_events.py

The serviceAccountKey.json file is in .gitignore — never commit it.
"""

import os
import random
import sys
import time
from datetime import datetime, timedelta

# ── Dependency check ──────────────────────────────────────────────────────────

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("Error: firebase-admin is not installed.")
    print("Run:  pip install firebase-admin")
    sys.exit(1)

# ── Firebase init ─────────────────────────────────────────────────────────────

KEY_PATH = os.path.join(os.path.dirname(__file__), "cosmos-discovery-firebase-adminsdk-fbsvc-1e9bae2712.json")

if not os.path.exists(KEY_PATH):
    print(f"Error: service account key not found at {KEY_PATH}")
    print("Download it from Firebase Console → Project Settings → Service Accounts.")
    sys.exit(1)

cred = credentials.Certificate(KEY_PATH)
firebase_admin.initialize_app(cred)
db = firestore.client()

# ── Helpers ───────────────────────────────────────────────────────────────────

def now_ms():
    return int(time.time() * 1000)

def days(n):
    """Returns n days in milliseconds."""
    return int(n * 24 * 60 * 60 * 1000)

def hours(n):
    """Returns n hours in milliseconds."""
    return int(n * 60 * 60 * 1000)

def at(day_offset, hour, minute=0):
    """Returns epoch ms for a specific day offset + time-of-day."""
    return NOW + days(day_offset) + hours(hour) + minute * 60 * 1000

def weekday_target(weekday, hour, minute=0):
    """
    Returns epoch ms for the upcoming (or today's) occurrence of `weekday` at the given time.
    weekday: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun (Python convention).
    If the target weekday is today, it returns today at that time.
    Used to anchor events to specific filter windows (TOMORROW, THIS_WEEKEND) regardless
    of when the script is run.
    """
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    days_ahead = (weekday - today.weekday()) % 7
    target = today + timedelta(days=days_ahead, hours=hour, minutes=minute)
    return int(target.timestamp() * 1000)

def register_by(epoch_ms, days_before=1):
    """Returns a 'yyyy-MM-dd' date string `days_before` days before the given epoch ms."""
    dt = datetime.fromtimestamp(epoch_ms / 1000) - timedelta(days=days_before)
    return dt.strftime("%Y-%m-%d")

def delete_collection(name):
    """Batch-deletes all documents in a collection (Firestore max 500 per batch)."""
    col_ref = db.collection(name)
    deleted = 0
    while True:
        docs = col_ref.limit(500).stream()
        doc_list = list(docs)
        if not doc_list:
            break
        batch = db.batch()
        for doc in doc_list:
            batch.delete(doc.reference)
        batch.commit()
        deleted += len(doc_list)
    return deleted

# ── Categories ────────────────────────────────────────────────────────────────

CATEGORIES = [
    "Technology",
    "Music",
    "Sports",
    "Academic",
    "Cultural",
    "Food",
    "Arts",
    "Workshop",
]

# ── Event data ────────────────────────────────────────────────────────────────
# dateTime is expressed as an offset from now() so every run stays relevant.
# Unsplash URLs: direct-download links keyed by topic for reliable image loading.

NOW = now_ms()

# Maps seed organizer IDs to display names for the organizerName field.
ORGANIZER_NAMES = {
    "seed_organizer_001": "Dr. Ahmed Raza",
    "seed_organizer_002": "Sara Malik",
    "seed_organizer_003": "Hassan Ali",
    "seed_organizer_004": "Fatima Sheikh",
    "seed_organizer_005": "Bilal Qureshi",
    "seed_organizer_006": "Zara Iqbal",
}

EVENTS = [
    # ── Upcoming, approved, lums_only ────────────────────────────────────────
    {
        "title":       "AI & Machine Learning Symposium",
        "description": "Explore cutting-edge AI research with talks from faculty and industry leaders. Covers deep learning, NLP, and computer vision.",
        "dateTime":    at(2, 10),        # 10:00am
        "location":    "SBASSE Auditorium",
        "tags":        ["Technology", "Academic"],
        "category":    "Technology",
        "capacity":    200,
        "imageUrl":    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
        "organizerId": "seed_organizer_001",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "LUMS Music Fest — Spring Edition",
        "description": "Live performances from student bands and guest artists. Food stalls, merch, and a headliner surprise act at 10pm.",
        "dateTime":    at(4, 19),        # 7:00pm
        "location":    "PDC Auditorium",
        "tags":        ["Music", "Cultural"],
        "category":    "Music",
        "capacity":    300,
        "imageUrl":    "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800",
        "organizerId": "seed_organizer_002",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Inter-University Cricket Tournament",
        "description": "Six universities compete in a T20 format over two days. Come cheer for the LUMS squad!",
        "dateTime":    at(5, 9),         # 9:00am
        "location":    "LUMS Sports Complex",
        "tags":        ["Sports"],
        "category":    "Sports",
        "imageUrl":    "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=800",
        "organizerId": "seed_organizer_003",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Startup Pitch Night",
        "description": "Student founders pitch to a panel of VCs and alumni investors. Top three teams win seed funding and mentorship.",
        "dateTime":    at(6, 18, 30),    # 6:30pm
        "location":    "SDSB Atrium",
        "tags":        ["Technology", "Workshop"],
        "category":    "Technology",
        "capacity":    80,
        "imageUrl":    "https://images.unsplash.com/photo-1551818255-e6e10975bc17?w=800",
        "organizerId": "seed_organizer_001",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Iqbal Day Poetry Recitation",
        "description": "An afternoon of Urdu and Persian poetry celebrating Allama Iqbal's legacy. Open mic session follows the featured readings.",
        "dateTime":    at(7, 16),        # 4:00pm
        "location":    "Social Sciences Block",
        "tags":        ["Cultural", "Academic"],
        "category":    "Cultural",
        "capacity":    60,
        "imageUrl":    "https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=800",
        "organizerId": "seed_organizer_004",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },

    # ── Upcoming, approved, open ──────────────────────────────────────────────
    {
        "title":       "Spring Food Festival",
        "description": "Lahore's best food trucks and campus societies serve up desi and fusion bites. Bring your appetite and your friends.",
        "dateTime":    at(3, 12),        # 12:00pm
        "location":    "LUMS Main Lawn",
        "tags":        ["Food", "Cultural"],
        "category":    "Food",
        "imageUrl":    "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800",
        "organizerId": "seed_organizer_002",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Photography Workshop",
        "description": "Hands-on session covering composition, lighting, and editing. Bring your own camera or phone. Beginner-friendly!",
        "dateTime":    at(8, 14),        # 2:00pm
        "location":    "Arts & Design Studio, AC-1",
        "tags":        ["Arts", "Workshop"],
        "category":    "Arts",
        "capacity":    40,
        "imageUrl":    "https://images.unsplash.com/photo-1452587925148-ce544e77e70d?w=800",
        "organizerId": "seed_organizer_005",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Hackathon 2026 — 24 Hours",
        "description": "Build something amazing in 24 hours. Teams of up to 4. Prizes include internships, cash, and gadgets.",
        "dateTime":    at(10, 20),       # 8:00pm
        "location":    "SBASSE Labs",
        "tags":        ["Technology", "Workshop"],
        "category":    "Technology",
        "capacity":    150,
        "imageUrl":    "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800",
        "organizerId": "seed_organizer_001",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Mental Health Awareness Walk",
        "description": "A peaceful morning walk around campus to raise awareness for student mental health. Free breakfast provided afterwards.",
        "dateTime":    at(12, 8),        # 8:00am
        "location":    "LUMS Main Lawn",
        "tags":        ["Academic", "Cultural"],
        "category":    "Academic",
        "imageUrl":    "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800",
        "organizerId": "seed_organizer_006",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Guest Lecture: Quantum Computing",
        "description": "Dr. Ayesha Khan from MIT discusses the state of quantum hardware and its implications for cryptography.",
        "dateTime":    at(14, 11),       # 11:00am
        "location":    "SBASSE Lecture Hall 3",
        "tags":        ["Technology", "Academic"],
        "category":    "Technology",
        "capacity":    120,
        "imageUrl":    "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800",
        "organizerId": "seed_organizer_003",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Campus Comedy Jam",
        "description": "Stand-up night featuring five student comedians and a surprise guest. Expect roasts, hot takes, and zero chill.",
        "dateTime":    at(9, 20, 30),    # 8:30pm
        "location":    "PDC Auditorium",
        "tags":        ["Music", "Cultural"],
        "category":    "Music",
        "capacity":    200,
        "imageUrl":    "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=800",
        "organizerId": "seed_organizer_002",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Badminton Singles Championship",
        "description": "Open to all skill levels. Single-elimination bracket. Medals and certificates for top 3 finishers.",
        "dateTime":    at(11, 15),       # 3:00pm
        "location":    "Indoor Sports Hall",
        "tags":        ["Sports"],
        "category":    "Sports",
        "capacity":    64,
        "imageUrl":    "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800",
        "organizerId": "seed_organizer_003",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Traditional Crafts Exhibition",
        "description": "Showcasing handmade crafts from artisans across Pakistan. Live demos of block printing, truck art, and pottery.",
        "dateTime":    at(15, 13),       # 1:00pm
        "location":    "SDSB Atrium",
        "tags":        ["Arts", "Cultural"],
        "category":    "Arts",
        "imageUrl":    "https://images.unsplash.com/photo-1509541206217-cde45c41aa6d?w=800",
        "organizerId": "seed_organizer_004",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },

    # ── Calendar-anchored events (always land in TOMORROW / THIS_WEEKEND filters) ──
    # weekday_target() pins each event to the correct weekday so re-running the
    # script on any day still produces events that hit these filter windows.
    {
        "title":       "Study Skills Masterclass",
        "description": "Learn effective note-taking, time management, and exam strategies from top academic achievers.",
        "dateTime":    weekday_target((datetime.now().weekday() + 1) % 7, 15),  # tomorrow 3:00pm
        "location":    "SDSB Lecture Hall 1",
        "tags":        ["Academic", "Workshop"],
        "category":    "Academic",
        "capacity":    50,
        "imageUrl":    "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800",
        "organizerId": "seed_organizer_005",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Saturday Astronomy Meetup",
        "description": "Stargazing session with telescopes on the main lawn. Learn to identify constellations visible from Lahore.",
        "dateTime":    weekday_target(5, 20),   # this Saturday 8:00pm
        "location":    "LUMS Main Lawn",
        "tags":        ["Technology", "Academic"],
        "category":    "Technology",
        "capacity":    75,
        "imageUrl":    "https://images.unsplash.com/photo-1446776653964-20c1d3a81b06?w=800",
        "organizerId": "seed_organizer_001",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Sunday Morning Yoga & Wellness",
        "description": "Guided yoga followed by a mindfulness session. Mats provided. All fitness levels welcome.",
        "dateTime":    weekday_target(6, 8),    # this Sunday 8:00am
        "location":    "LUMS Sports Complex",
        "tags":        ["Sports", "Cultural"],
        "category":    "Sports",
        "capacity":    40,
        "imageUrl":    "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        "organizerId": "seed_organizer_006",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },

    # ── Past, approved ────────────────────────────────────────────────────────
    {
        "title":       "Winter Gala Night",
        "description": "A formal evening of music, dinner, and awards. Celebrating another great semester at LUMS.",
        "dateTime":    at(-10, 19),      # 7:00pm
        "location":    "PDC Auditorium",
        "tags":        ["Music", "Cultural"],
        "category":    "Music",
        "capacity":    250,
        "imageUrl":    "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800",
        "organizerId": "seed_organizer_002",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },
    {
        "title":       "Robotics Demo Day",
        "description": "Student teams showcase their semester-long robotics projects. Judges award prizes for innovation and design.",
        "dateTime":    at(-5, 14, 30),   # 2:30pm
        "location":    "SBASSE Labs",
        "tags":        ["Technology", "Workshop"],
        "category":    "Technology",
        "capacity":    100,
        "imageUrl":    "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800",
        "organizerId": "seed_organizer_001",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Annual Sports Day",
        "description": "Track and field events, tug of war, and relay races. Hostels compete for the overall championship trophy.",
        "dateTime":    at(-15, 9),       # 9:00am
        "location":    "LUMS Sports Complex",
        "tags":        ["Sports"],
        "category":    "Sports",
        "capacity":    500,
        "imageUrl":    "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800",
        "organizerId": "seed_organizer_003",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Alumni Networking Dinner",
        "description": "Connect with LUMS alumni working in tech, finance, and consulting. Formal attire required.",
        "dateTime":    at(-3, 18),       # 6:00pm
        "location":    "Faculty Club",
        "tags":        ["Academic"],
        "category":    "Academic",
        "capacity":    80,
        "imageUrl":    "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800",
        "organizerId": "seed_organizer_006",
        "status":      "approved",
        "attendeeIds": [],
        "accessType":  "open",
    },

    # ── Upcoming, pending (not yet approved — won't show in Discover) ─────────
    {
        "title":       "Debating Society Open Mic",
        "description": "Sharpen your rhetoric at this casual open-mic debate night. Topics announced on the spot.",
        "dateTime":    at(20, 17),       # 5:00pm
        "location":    "Social Sciences Block",
        "tags":        ["Academic", "Cultural"],
        "category":    "Academic",
        "capacity":    100,
        "imageUrl":    "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800",
        "organizerId": "seed_organizer_005",
        "status":      "pending",
        "attendeeIds": [],
        "accessType":  "lums_only",
    },
    {
        "title":       "Pottery & Ceramics Workshop",
        "description": "Get your hands dirty! Learn wheel-throwing and hand-building techniques. All materials provided.",
        "dateTime":    at(18, 11),       # 11:00am
        "location":    "Arts & Design Studio, AC-1",
        "tags":        ["Arts", "Workshop"],
        "category":    "Arts",
        "capacity":    25,
        "imageUrl":    "https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=800",
        "organizerId": "seed_organizer_004",
        "status":      "pending",
        "attendeeIds": [],
        "accessType":  "open",
    },

    # ── Past, rejected (won't appear anywhere in the app) ────────────────────
    {
        "title":       "Off-Campus Concert Trip",
        "description": "Group trip to a live concert downtown. Transport arranged by the music society.",
        "dateTime":    at(-20, 21),      # 9:00pm
        "location":    "Liberty Roundabout, Lahore",
        "tags":        ["Music"],
        "category":    "Music",
        "imageUrl":    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800",
        "organizerId": "seed_organizer_002",
        "status":      "rejected",
        "attendeeIds": [],
        "accessType":  "open",
    },
]

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    now = now_ms()

    print("🗑  Deleting events collection...", end="  ", flush=True)
    n = delete_collection("events")
    print(f"done ({n} docs deleted)")

    print("🗑  Deleting categories collection...", end="  ", flush=True)
    n = delete_collection("categories")
    print(f"done ({n} docs deleted)")

    # Seed categories
    col = db.collection("categories")
    for name in CATEGORIES:
        col.add({"name": name})
    print(f"✅ Seeded {len(CATEGORIES)} categories")

    # Seed events — auto-fill createdAt, registerBy, and dateOfEvent
    col = db.collection("events")
    for event in EVENTS:
        doc = dict(event)
        doc["createdAt"] = now
        doc["rsvpCount"] = len(doc.get("attendeeIds", []))
        doc["organizerName"] = ORGANIZER_NAMES.get(doc.get("organizerId", ""), "Unknown Organizer")
        dt = datetime.fromtimestamp(doc["dateTime"] / 1000)
        if "registerBy" not in doc:
            doc["registerBy"] = (dt - timedelta(days=1)).strftime("%Y-%m-%d")
        if "dateOfEvent" not in doc:
            doc["dateOfEvent"] = dt.strftime("%Y-%m-%d")
        col.add(doc)
    print(f"✅ Seeded {len(EVENTS)} events")

    upcoming = sum(1 for e in EVENTS if e["dateTime"] > now and e["status"] == "approved")
    past     = sum(1 for e in EVENTS if e["dateTime"] <= now and e["status"] == "approved")
    pending  = sum(1 for e in EVENTS if e["status"] == "pending")
    print()
    print(f"   {upcoming} upcoming (approved)  |  {past} past (approved)  |  {pending} pending  |  1 rejected")
    print()
    print("🎉 Done! Open the app and check the Discover and My Events tabs.")

if __name__ == "__main__":
    main()
