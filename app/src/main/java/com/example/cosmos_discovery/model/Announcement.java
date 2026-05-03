package com.example.cosmos_discovery.model;

/**
 * One organizer-sent announcement attached to an event.
 * Stored at {@code events/{eventId}/announcements/{auto-id}}.
 *
 * The {@code id} is the Firestore document ID; it is set after fetch and is not
 * persisted as a field. Field names map 1:1 to Firestore.
 */
public class Announcement {

    public static final int MAX_LENGTH = 300;

    private String id;            // doc ID — not stored
    private String message;
    private long   sentAt;
    private String organizerId;

    /** Required by Firestore for deserialization. */
    public Announcement() {}

    public Announcement(String message, long sentAt, String organizerId) {
        this.message     = message;
        this.sentAt      = sentAt;
        this.organizerId = organizerId;
    }

    public String getId()                 { return id; }
    public void   setId(String v)         { id = v; }

    public String getMessage()            { return message; }
    public void   setMessage(String v)    { message = v; }

    public long   getSentAt()             { return sentAt; }
    public void   setSentAt(long v)       { sentAt = v; }

    public String getOrganizerId()        { return organizerId; }
    public void   setOrganizerId(String v) { organizerId = v; }
}
