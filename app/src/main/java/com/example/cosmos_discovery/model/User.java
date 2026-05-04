package com.example.cosmos_discovery.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore-mapped model for an application user profile.
 *
 * <p>Field names map 1:1 to Firestore document fields under the {@code users}
 * collection (keyed by Firebase Auth UID). Renaming any field in this class
 * will break deserialization of existing documents — migrate the data first.
 *
 * <p>The no-arg constructor is required by the Firestore SDK for
 * deserialization. Application code should use
 * {@link #User(String, String, String)} instead.
 *
 * @author Hafsah Nasir
 * @version 1.0
 * @since 1.0
 */
public class User {

    /** Role value for student users. */
    public static final String ROLE_STUDENT   = "student";
    /** Role value for organizer users. */
    public static final String ROLE_ORGANIZER = "organizer";
    /** Role value for admin users. */
    public static final String ROLE_ADMIN     = "admin";

    private String  uid;
    private String  name;
    private String  email;
    private String  role;
    private boolean isActive;
    private String  batch;
    private String  major;
    private String  bio;
    private long    createdAt;
    private String  photoUrl;

    /** Set when an organizer request is approved; cleared after the promotion popup is dismissed. */
    private boolean promotionApproved;

    /** FCM device token used for push notifications; {@code null} until first registration. */
    private String fcmToken;

    /** Per-category notification preferences. {@code null} on legacy docs — treated as all-enabled. */
    private Map<String, Boolean> notifPrefs;

    /** Required by the Firestore SDK for deserialization. Do not use in application code. */
    public User() {}

    /**
     * Creates a new user with the {@link #ROLE_STUDENT} role and an active account.
     *
     * @param uid          Firebase Auth UID — primary key for the {@code users} document.
     * @param email        Email address used to sign in (must be a {@code @lums.edu.pk} address).
     * @param displayName  Human-readable display name.
     */
    public User(String uid, String email, String displayName) {
        this.uid        = uid;
        this.email      = email;
        this.name       = displayName;
        this.role       = ROLE_STUDENT;
        this.isActive   = true;
        this.createdAt  = System.currentTimeMillis();
        this.notifPrefs = defaultNotifPrefs();
    }

    /**
     * Returns the default per-category notification preferences (all enabled) used for
     * new users. The notification Cloud Function treats missing keys as {@code true}.
     */
    public static Map<String, Boolean> defaultNotifPrefs() {
        Map<String, Boolean> prefs = new HashMap<>();
        prefs.put("push",           true);
        prefs.put("rsvp",           true);
        prefs.put("eventUpdates",   true);
        prefs.put("announcements",  true);
        prefs.put("friendRequests", true);
        prefs.put("adminDecisions", true);
        prefs.put("capacityFull",   true);
        prefs.put("rsvpReceived",   true);
        return prefs;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getUid()                 { return uid; }
    public void   setUid(String uid)       { this.uid = uid; }

    public String getName()                { return name; }
    public void   setName(String name)     { this.name = name; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

    public boolean isActive()              { return isActive; }
    public void    setActive(boolean v)    { isActive = v; }

    public String getRole()                { return role; }
    public void   setRole(String role)     { this.role = role; }

    public String getBatch()               { return batch; }
    public void   setBatch(String batch)   { this.batch = batch; }

    public String getMajor()               { return major; }
    public void   setMajor(String major)   { this.major = major; }

    public String getBio()                 { return bio; }
    public void   setBio(String bio)       { this.bio = bio; }

    public long getCreatedAt()             { return createdAt; }
    public void setCreatedAt(long ts)      { this.createdAt = ts; }

    public String getPhotoUrl()            { return photoUrl; }
    public void   setPhotoUrl(String url)  { this.photoUrl = url; }

    public boolean isPromotionApproved()           { return promotionApproved; }
    public void    setPromotionApproved(boolean v) { this.promotionApproved = v; }

    public String getFcmToken()                    { return fcmToken; }
    public void   setFcmToken(String fcmToken)     { this.fcmToken = fcmToken; }

    public Map<String, Boolean> getNotifPrefs()                       { return notifPrefs; }
    public void                 setNotifPrefs(Map<String, Boolean> p) { this.notifPrefs = p; }
}
