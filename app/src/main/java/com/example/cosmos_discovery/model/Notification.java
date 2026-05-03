package com.example.cosmos_discovery.model;

public class Notification {

    // Type constants — used by adapter to pick icon
    public static final String TYPE_FRIEND_REQUEST_RECEIVED = "friend_request_received";
    public static final String TYPE_FRIEND_REQUEST_ACCEPTED = "friend_request_accepted";
    public static final String TYPE_RSVP_CONFIRMED          = "rsvp_confirmed";
    public static final String TYPE_EVENT_UPDATED           = "event_updated";
    public static final String TYPE_EVENT_CANCELLED         = "event_cancelled";

    private String  id;         // set post-fetch, NOT stored in Firestore
    private String  type;
    private String  title;
    private String  message;
    private long    timestamp;
    private boolean read;
    private String  eventId;    // nullable
    private String  eventTitle; // nullable

    /** Required by Firestore toObject(). */
    public Notification() {}

    public Notification(String type, String title, String message, long timestamp) {
        this.type      = type;
        this.title     = title;
        this.message   = message;
        this.timestamp = timestamp;
        this.read      = false;
    }

    public String  getId()              { return id; }
    public void    setId(String v)      { id = v; }

    public String  getType()            { return type; }
    public void    setType(String v)    { type = v; }

    public String  getTitle()           { return title; }
    public void    setTitle(String v)   { title = v; }

    public String  getMessage()         { return message; }
    public void    setMessage(String v) { message = v; }

    public long    getTimestamp()         { return timestamp; }
    public void    setTimestamp(long v)   { timestamp = v; }

    public boolean isRead()              { return read; }
    public void    setRead(boolean v)    { read = v; }

    public String  getEventId()          { return eventId; }
    public void    setEventId(String v)  { eventId = v; }

    public String  getEventTitle()          { return eventTitle; }
    public void    setEventTitle(String v)  { eventTitle = v; }
}
