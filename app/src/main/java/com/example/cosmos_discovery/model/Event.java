package com.example.cosmos_discovery.model;

import java.util.List;

/**
 * Firestore-mapped model for a campus event.
 *
 * Field names map 1:1 to Firestore document fields — never rename without migrating data.
 * The empty constructor is required by the Firestore SDK for deserialization; do not use it
 * in application code.
 *
 * {@code id} is the Firestore document ID — it is set after fetch via {@link #setId(String)}
 * and is NOT stored as a field inside the document itself.
 */
public class Event {

    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_REJECTED = "rejected";

    private String       id;          // doc ID — set post-fetch, not a Firestore field
    private String       title;
    private long         dateTime;    // epoch ms
    private String       location;
    private List<String> tags;
    private int          rsvpCount;
    private String       imageUrl;
    private String       organizerId;
    private String       status;
    private List<String> attendeeIds;
    private List<String> checkedInIds;
    private long         createdAt;
    private String       accessType;  // "lums_only" | "open" | null (null treated as open)
    private String       description;
    private String       category;
    private String       dateOfEvent; // raw form value (display), e.g. "2026-04-06"
    private String       startTime;   // raw form value, e.g. "5:00pm"
    private String       endTime;     // raw form value, e.g. "7:00pm"
    private String       registerBy;  // raw form value (display)

    /** Required by Firestore for deserialization. Do not use in application code. */
    protected Event() {}

    /**
     * Creates a new pending event. {@code rsvpCount} defaults to 0 and
     * {@code status} defaults to {@link #STATUS_PENDING}.
     */
    public Event(String title, long dateTime, String location, List<String> tags,
                 String imageUrl, String organizerId) {
        this.title       = title;
        this.dateTime    = dateTime;
        this.location    = location;
        this.tags        = tags;
        this.imageUrl    = imageUrl;
        this.organizerId = organizerId;
        this.status      = STATUS_PENDING;
        this.rsvpCount   = 0;
        this.createdAt   = System.currentTimeMillis();
    }

    /** Returns true if the given user has RSVPed to this event. */
    public boolean isRsvped(String userId) {
        return attendeeIds != null && attendeeIds.contains(userId);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getId()                    { return id; }
    public void   setId(String id)           { this.id = id; }

    public String getTitle()                 { return title; }
    public void   setTitle(String title)     { this.title = title; }

    public long   getDateTime()              { return dateTime; }
    public void   setDateTime(long dateTime) { this.dateTime = dateTime; }

    public String getLocation()              { return location; }
    public void   setLocation(String loc)   { this.location = loc; }

    public List<String> getTags()            { return tags; }
    public void         setTags(List<String> tags) { this.tags = tags; }

    public int  getRsvpCount()               { return rsvpCount; }
    public void setRsvpCount(int count)      { this.rsvpCount = count; }

    public String getImageUrl()              { return imageUrl; }
    public void   setImageUrl(String url)    { this.imageUrl = url; }

    public String getOrganizerId()           { return organizerId; }
    public void   setOrganizerId(String id)  { this.organizerId = id; }

    public String getStatus()                { return status; }
    public void   setStatus(String status)   { this.status = status; }

    public List<String> getAttendeeIds()     { return attendeeIds; }
    public void setAttendeeIds(List<String> ids) { this.attendeeIds = ids; }

    public List<String> getCheckedInIds()    { return checkedInIds; }
    public void setCheckedInIds(List<String> ids) { this.checkedInIds = ids; }

    public long getCreatedAt()               { return createdAt; }
    public void setCreatedAt(long ts)        { this.createdAt = ts; }

    public String getAccessType()                    { return accessType; }
    public void   setAccessType(String accessType)   { this.accessType = accessType; }

    public String getDescription()                   { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getCategory()                      { return category; }
    public void   setCategory(String category)       { this.category = category; }

    public String getDateOfEvent()                   { return dateOfEvent; }
    public void   setDateOfEvent(String dateOfEvent) { this.dateOfEvent = dateOfEvent; }

    public String getStartTime()                     { return startTime; }
    public void   setStartTime(String startTime)     { this.startTime = startTime; }

    public String getEndTime()                       { return endTime; }
    public void   setEndTime(String endTime)         { this.endTime = endTime; }

    public String getRegisterBy()                    { return registerBy; }
    public void   setRegisterBy(String registerBy)   { this.registerBy = registerBy; }
}
