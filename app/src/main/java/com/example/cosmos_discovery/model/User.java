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

    /**
     * Default constructor required for serialization/deserialization frameworks.
     */
    protected User(){}

    /**
     * Creates a user with default initial values.
     *
     * @param uid Unique identifier of the user.
     * @param email Email address of the user.
     * @param displayName Display name of the user.
     */
    public User(String uid, String email, String displayName){
        this.uid         = uid;
        this.email       = email;
        this.name        = displayName;
        this.role        = ROLE_STUDENT;   // default role
        this.isActive    = true;
        this.createdAt   = System.currentTimeMillis();
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return The user id.
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the user's unique identifier.
     *
     * @param uid The user id to set.
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * Returns the user's display name.
     *
     * @return The user name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's display name.
     *
     * @param name The display name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user's email address.
     *
     * @return The email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The email address to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns whether the user account is active.
     *
     * @return True if active; otherwise false.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets whether the user account is active.
     *
     * @param active The active status to set.
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Returns the role assigned to the user.
     *
     * @return The user's role.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role assigned to the user.
     *
     * @param role The role to set.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Returns the user's academic batch.
     *
     * @return The batch.
     */
    public String getBatch() {
        return batch;
    }

    /**
     * Sets the user's academic batch.
     *
     * @param batch The batch to set.
     */
    public void setBatch(String batch) {
        this.batch = batch;
    }

    /**
     * Returns the user's major.
     *
     * @return The major.
     */
    public String getMajor() {
        return major;
    }

    /**
     * Sets the user's major.
     *
     * @param major The major to set.
     */
    public void setMajor(String major) {
        this.major = major;
    }

    /**
     * Returns the user's biography.
     *
     * @return The bio text.
     */
    public String getBio() {
        return bio;
    }

    /**
     * Sets the user's biography.
     *
     * @param bio The bio text to set.
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Returns the creation timestamp of the user record.
     *
     * @return The creation time in milliseconds since epoch.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the user record.
     *
     * @param createdAt The creation time in milliseconds since epoch.
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
