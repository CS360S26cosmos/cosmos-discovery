package com.example.cosmos_discovery.model;

/** Firestore-mapped POJO for the {@code users/{uid}/friends/{friendUid}} subcollection. */
public class FriendEntry {

    private String uid;
    private String name;
    private String photoUrl;
    private long   addedAt;

    /** Required by Firestore for deserialization. Do not use in application code. */
    protected FriendEntry() {}

    public FriendEntry(String uid, String name, String photoUrl) {
        this.uid      = uid;
        this.name     = name;
        this.photoUrl = photoUrl;
        this.addedAt  = System.currentTimeMillis();
    }

    public String getUid()      { return uid; }
    public void   setUid(String uid) { this.uid = uid; }

    public String getName()      { return name; }
    public void   setName(String name) { this.name = name; }

    public String getPhotoUrl()       { return photoUrl; }
    public void   setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getAddedAt()          { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
}
