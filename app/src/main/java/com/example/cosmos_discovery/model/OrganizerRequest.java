package com.example.cosmos_discovery.model;

public class OrganizerRequest {
    public static final String STATUS_PENDING = "pending";

    private String userId;
    private String userName;
    private String userEmail;
    private String userPhotoUrl;
    private String status;
    private long   createdAt;

    public OrganizerRequest() {}

    public OrganizerRequest(String userId, String userName,
                             String userEmail, String userPhotoUrl) {
        this.userId       = userId;
        this.userName     = userName;
        this.userEmail    = userEmail;
        this.userPhotoUrl = userPhotoUrl;
        this.status       = STATUS_PENDING;
        this.createdAt    = System.currentTimeMillis();
    }

    public String getUserId()       { return userId; }
    public void   setUserId(String v) { userId = v; }

    public String getUserName()       { return userName; }
    public void   setUserName(String v) { userName = v; }

    public String getUserEmail()       { return userEmail; }
    public void   setUserEmail(String v) { userEmail = v; }

    public String getUserPhotoUrl()       { return userPhotoUrl; }
    public void   setUserPhotoUrl(String v) { userPhotoUrl = v; }

    public String getStatus()       { return status; }
    public void   setStatus(String v) { status = v; }

    public long  getCreatedAt()      { return createdAt; }
    public void  setCreatedAt(long v) { createdAt = v; }
}
