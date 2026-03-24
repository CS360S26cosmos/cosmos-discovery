package com.example.cosmos_discovery.model;

/**
 * Represents an application user profile.
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



    /** Unique identifier of the user. */
    private String uid;
    /** Display name of the user. */
    private String name;
    /** Email address used by the user. */
    private String email;
    /** Current role assigned to the user. */
    private String role;
    /** Indicates whether the user account is active. */
    private boolean isActive;
    /** Academic batch of the user. */
    private String batch;
    /** Major or department of study. */
    private String major;
    /** Short biography or profile description. */
    private String bio;
    /** Timestamp when the user record was created. */
    private long createdAt;

    private User(){}

    public User(String uid, String email, String displayName){
        this.uid         = uid;
        this.email       = email;
        this.name        = displayName;
        this.role        = ROLE_STUDENT;   // default role
        this.isActive    = true;
        this.createdAt   = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
