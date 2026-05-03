"""
Cosmos Discovery — User & Friends seed script
==============================================
Creates 12 test students with profile photos uploaded from local image files, friends
them randomly with the three existing users shown in the app screenshot, makes
some of them friends with each other, and RSVPs them to a random mix of
approved past and upcoming events.

Idempotent: if a seeded email already exists in Firebase Auth the script
skips that user and continues — safe to re-run.

SETUP:
  pip install firebase-admin
  python scripts/seed_users.py

The service account key is already present at:
  scripts/cosmos-discovery-firebase-adminsdk-fbsvc-1e9bae2712.json
"""

import os
import sys
import time
import random
import uuid
import urllib.parse

# ── Dependency check ───────────────────────────────────────────────────────────

try:
    import firebase_admin
    from firebase_admin import credentials, firestore, auth, storage
    from google.cloud.firestore_v1 import ArrayUnion, Increment
except ImportError as e:
    print(f"Error: missing dependency — {e}")
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

cred = credentials.Certificate(KEY_PATH)
firebase_admin.initialize_app(cred, {"storageBucket": "cosmos-discovery.firebasestorage.app"})
db = firestore.client()

# ── Image paths ────────────────────────────────────────────────────────────────

IMAGES_DIR = "/Users/hafsahnasir/Developer/SE_Project/images"
MALE_IMAGES   = ["male1.png",   "male2.png"]
FEMALE_IMAGES = ["female1.png", "female2.png"]


# ── Helpers ────────────────────────────────────────────────────────────────────

def now_ms():
    return int(time.time() * 1000)


def upload_profile_image(local_filename, storage_path):
    """
    Uploads a local image file to Firebase Storage and returns a permanent
    download URL using the download-token approach (works without public IAM).
    Re-uses an existing blob (skips upload) if one already exists at storage_path.
    """
    bucket = storage.bucket()
    blob   = bucket.blob(storage_path)

    # Skip re-upload if already there — token survives re-runs
    if blob.exists():
        # Reload metadata to read the existing token
        blob.reload()
        token = (blob.metadata or {}).get("firebaseStorageDownloadTokens")
        if token:
            encoded = urllib.parse.quote(storage_path, safe="")
            return (
                f"https://firebasestorage.googleapis.com/v0/b/"
                f"{bucket.name}/o/{encoded}?alt=media&token={token}"
            )

    # Upload with a fresh download token
    token = str(uuid.uuid4())
    local_path = os.path.join(IMAGES_DIR, local_filename)
    blob.upload_from_filename(local_path, content_type="image/png")
    blob.metadata = {"firebaseStorageDownloadTokens": token}
    blob.patch()

    encoded = urllib.parse.quote(storage_path, safe="")
    return (
        f"https://firebasestorage.googleapis.com/v0/b/"
        f"{bucket.name}/o/{encoded}?alt=media&token={token}"
    )


# ── Seed data ──────────────────────────────────────────────────────────────────

# The three existing users from the screenshot — their UIDs are fetched at runtime.
EXISTING_USER_EMAILS = [
    "27100337@lums.edu.pk",   # sameen abid
    "27100026@lums.edu.pk",   # Hamania Asim
    "27100088@lums.edu.pk",   # Ammara Haroon
    "27100052@lums.edu.pk",    # Elizeh Faisal
    "27100237@lums.edu.pk"    # Hafsah Nasir
]

# 12 new seed students — sex determines which local image pool to pick from
SEED_USERS = [
    {"name": "Ahmed Khan",      "email": "27100201@lums.edu.pk", "sex": "male",   "major": "Computer Science",       "batch": "2027"},
    {"name": "Fatima Malik",    "email": "27100202@lums.edu.pk", "sex": "female", "major": "Economics",              "batch": "2027"},
    {"name": "Omar Sheikh",     "email": "27100203@lums.edu.pk", "sex": "male",   "major": "Physics",                "batch": "2026"},
    {"name": "Ayesha Raza",     "email": "27100204@lums.edu.pk", "sex": "female", "major": "Computer Science",       "batch": "2026"},
    {"name": "Ali Hassan",      "email": "27100205@lums.edu.pk", "sex": "male",   "major": "Mathematics",            "batch": "2027"},
    {"name": "Sara Qureshi",    "email": "27100206@lums.edu.pk", "sex": "female", "major": "Management Science",     "batch": "2025"},
    {"name": "Bilal Ahmed",     "email": "27100207@lums.edu.pk", "sex": "male",   "major": "Electrical Engineering", "batch": "2026"},
    {"name": "Zara Hussain",    "email": "27100208@lums.edu.pk", "sex": "female", "major": "Accounting & Finance",   "batch": "2027"},
    {"name": "Usman Tariq",     "email": "27100209@lums.edu.pk", "sex": "male",   "major": "Law",                    "batch": "2025"},
    {"name": "Maryam Khan",     "email": "27100210@lums.edu.pk", "sex": "female", "major": "Computer Science",       "batch": "2026"},
    {"name": "Danish Siddiqui", "email": "27100211@lums.edu.pk", "sex": "male",   "major": "Biology",                "batch": "2027"},
    {"name": "Hina Baig",       "email": "27100212@lums.edu.pk", "sex": "female", "major": "Arts & Humanities",      "batch": "2025"},
]

# Password used for all seed accounts (satisfies the app's isValidPassword rules).
SEED_PASSWORD = "SeedPass1!"


# ── Core functions ─────────────────────────────────────────────────────────────

def delete_subcollection(uid, subcol):
    """Batch-deletes all docs in users/{uid}/{subcol}."""
    ref = db.collection("users").document(uid).collection(subcol)
    docs = list(ref.stream())
    if not docs:
        return
    batch = db.batch()
    for doc in docs:
        batch.delete(doc.reference)
    batch.commit()


def delete_seed_user(email):
    """
    Fully removes one previously seeded user:
      1. Reads their friends subcollection to find reverse entries to clean up.
      2. Deletes the reverse friendship doc from each friend's subcollection.
      3. Deletes every doc in users/{uid}/friends.
      4. Deletes every doc in users/{uid}/friendRequests (incoming requests).
      5. Deletes every doc in users/{uid}/sentRequests (outgoing requests).
      6. Deletes the reverse sentRequests entries from users who received a
         request from this seed user (so their incoming queue is also clean).
      7. Deletes the users/{uid} Firestore document.
      8. Deletes the Firebase Auth account.
    Only acts if the email belongs to a SEED_USER — never touches existing real users.
    """
    uid = get_uid_by_email(email)
    if uid is None:
        return  # not in Auth — nothing to delete

    user_ref = db.collection("users").document(uid)

    # 1 + 2: collect friends, delete reverse friendship entries
    friend_docs = list(user_ref.collection("friends").stream())
    for fdoc in friend_docs:
        db.collection("users").document(fdoc.id).collection("friends").document(uid).delete()

    # 3: delete own friends subcollection
    batch = db.batch()
    for fdoc in friend_docs:
        batch.delete(fdoc.reference)
    if friend_docs:
        batch.commit()

    # 4 + 6: delete incoming friend requests; also remove the mirror sentRequests
    #         entry from each sender so their outgoing queue is clean
    req_docs = list(user_ref.collection("friendRequests").stream())
    for rdoc in req_docs:
        sender_uid = rdoc.id
        db.collection("users").document(sender_uid).collection("sentRequests").document(uid).delete()
    delete_subcollection(uid, "friendRequests")

    # 5 + reverse: delete outgoing sent requests; also remove the mirror
    #              friendRequests entry from each target
    sent_docs = list(user_ref.collection("sentRequests").stream())
    for sdoc in sent_docs:
        target_uid = sdoc.id
        db.collection("users").document(target_uid).collection("friendRequests").document(uid).delete()
    delete_subcollection(uid, "sentRequests")

    # 7: delete Firestore user document
    user_ref.delete()

    # 8: delete Firebase Auth account
    try:
        auth.delete_user(uid)
    except auth.UserNotFoundError:
        pass  # already gone


def get_uid_by_email(email):
    """Returns the Firebase Auth UID for an email, or None if not found."""
    try:
        return auth.get_user_by_email(email).uid
    except auth.UserNotFoundError:
        return None


def create_auth_user(user):
    """
    Creates a Firebase Auth user and returns the UID.
    If the email already exists, returns the existing UID instead.
    """
    existing_uid = get_uid_by_email(user["email"])
    if existing_uid:
        return existing_uid, False  # (uid, is_new)

    record = auth.create_user(
        email=user["email"],
        password=SEED_PASSWORD,
        display_name=user["name"],
        email_verified=True,       # Admin SDK bypasses the email gate
    )
    return record.uid, True


def write_user_doc(uid, user, photo_url):
    """Writes (or overwrites) the Firestore user document."""
    db.collection("users").document(uid).set({
        "uid":               uid,
        "name":              user["name"],
        "email":             user["email"],
        "role":              "student",
        "active":            True,   # matches User.isActive() getter → Firestore field "active"
        "batch":             user["batch"],
        "major":             user["major"],
        "bio":               "",
        "createdAt":         now_ms(),
        "photoUrl":          photo_url,
        "promotionApproved": False,
    })


def write_friendship(a, b):
    """
    Writes both sides of a friendship atomically.
    a / b are dicts with keys: uid, name, photoUrl.
    Skips if the friendship document already exists.
    """
    ref_a = (db.collection("users").document(a["uid"])
               .collection("friends").document(b["uid"]))
    ref_b = (db.collection("users").document(b["uid"])
               .collection("friends").document(a["uid"]))

    # Skip if already friends
    if ref_a.get().exists:
        return False

    batch = db.batch()
    batch.set(ref_a, {
        "uid":      b["uid"],
        "name":     b["name"],
        "photoUrl": b["photoUrl"],
        "addedAt":  now_ms(),
    })
    batch.set(ref_b, {
        "uid":      a["uid"],
        "name":     a["name"],
        "photoUrl": a["photoUrl"],
        "addedAt":  now_ms(),
    })
    batch.commit()
    return True


def rsvp_user_to_event(uid, event_ref):
    """Adds uid to event.attendeeIds and increments rsvpCount."""
    event_ref.update({
        "attendeeIds": ArrayUnion([uid]),
        "rsvpCount":   Increment(1),
    })


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    random.seed(42)   # reproducible randomness

    # ── Step 0: Delete previously seeded users ────────────────────────────────
    print("🗑  Removing previously seeded users...")
    for user in SEED_USERS:
        delete_seed_user(user["email"])
        print(f"   ✓  {user['name']}")
    print(f"   Done — {len(SEED_USERS)} users cleaned up\n")

    # ── Step 1: Fetch existing user profiles ──────────────────────────────────
    print("🔍  Looking up existing users...")
    existing_profiles = []
    for email in EXISTING_USER_EMAILS:
        uid = get_uid_by_email(email)
        if uid is None:
            print(f"   ⚠  {email} not found in Firebase Auth — skipping")
            continue
        doc = db.collection("users").document(uid).get()
        if not doc.exists:
            print(f"   ⚠  Firestore doc missing for {email} — skipping")
            continue
        data = doc.to_dict()
        existing_profiles.append({
            "uid":      uid,
            "name":     data.get("name", email),
            "photoUrl": data.get("photoUrl"),
        })
        print(f"   ✓  {data.get('name', email)}  ({uid[:8]}...)")

    if not existing_profiles:
        print("   No existing users found — friendship step will be skipped.")

    # ── Step 2: Fetch approved events for RSVPs ────────────────────────────────
    print("\n📅  Fetching approved events...")
    events_snap = (
        db.collection("events")
          .where("status", "==", "approved")
          .stream()
    )
    event_refs = []
    for doc in events_snap:
        event_refs.append(db.collection("events").document(doc.id))
    print(f"   Found {len(event_refs)} approved events")

    # ── Step 3: Upload profile images to Firebase Storage ─────────────────────
    print("\n🖼  Uploading profile images...")
    male_urls   = []
    female_urls = []
    for filename in MALE_IMAGES:
        url = upload_profile_image(filename, f"seed_profile_images/{filename}")
        male_urls.append(url)
        print(f"   ✓  {filename}")
    for filename in FEMALE_IMAGES:
        url = upload_profile_image(filename, f"seed_profile_images/{filename}")
        female_urls.append(url)
        print(f"   ✓  {filename}")

    male_idx   = 0
    female_idx = 0

    # ── Step 4: Create seed users ──────────────────────────────────────────────
    print("\n👤  Creating seed users...")
    created_profiles = []  # dicts with uid, name, photoUrl — for later friendship wiring

    for user in SEED_USERS:
        if user["sex"] == "male":
            url = male_urls[male_idx % len(male_urls)]
            male_idx += 1
        else:
            url = female_urls[female_idx % len(female_urls)]
            female_idx += 1

        uid, is_new = create_auth_user(user)
        write_user_doc(uid, user, url)
        profile = {
            "uid":      uid,
            "name":     user["name"],
            "photoUrl": url,
        }
        created_profiles.append(profile)
        status = "created" if is_new else "updated"
        print(f"   ✓  {user['name']:20s}  {uid[:8]}...  [{status}]")

    # ── Step 5: Friend new users with existing users ───────────────────────────
    print("\n🤝  Wiring friendships (new ↔ existing)...")
    friendship_count = 0

    for profile in created_profiles:
        # Each new user randomly befriends 1–3 of the existing users
        k = random.randint(1, min(3, len(existing_profiles)))
        chosen = random.sample(existing_profiles, k)
        for existing in chosen:
            if write_friendship(profile, existing):
                friendship_count += 1
                print(f"   ✓  {profile['name']} ↔ {existing['name']}")

    # ── Step 6: Friend some new users with each other ─────────────────────────
    print("\n🤝  Wiring friendships (new ↔ new)...")
    # Build ~6 random pairs among the 12 new users
    indices = list(range(len(created_profiles)))
    random.shuffle(indices)
    for i in range(0, min(12, len(indices) - 1), 2):
        a = created_profiles[indices[i]]
        b = created_profiles[indices[i + 1]]
        if write_friendship(a, b):
            friendship_count += 1
            print(f"   ✓  {a['name']} ↔ {b['name']}")

    # ── Step 7: RSVP new users to random events ────────────────────────────────
    print("\n🎟  RSVPing users to events...")
    rsvp_count = 0

    if event_refs:
        for profile in created_profiles:
            # Each user RSVPs to 2–4 random events
            k = random.randint(2, min(4, len(event_refs)))
            chosen_events = random.sample(event_refs, k)
            for event_ref in chosen_events:
                rsvp_user_to_event(profile["uid"], event_ref)
                rsvp_count += 1
            print(f"   ✓  {profile['name']:20s}  RSVPed to {k} events")
    else:
        print("   ⚠  No events found — run seed_events.py first")

    # ── Summary ───────────────────────────────────────────────────────────────
    print()
    print(f"🎉  Done!")
    print(f"   {len(created_profiles)} users seeded")
    print(f"   {friendship_count} friendships written")
    print(f"   {rsvp_count} RSVPs written")
    print()
    print("Open the app, sign in,")
    print("then check the Friends tab — you should see their new friends' avatars")
    print("and events their friends have RSVP'd to.")


if __name__ == "__main__":
    main()
