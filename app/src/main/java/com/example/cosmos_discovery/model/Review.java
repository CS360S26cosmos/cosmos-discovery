package com.example.cosmos_discovery.model;

public class Review {
    public final String userId;
    public final String name;
    public final int    rating;
    public final String text;

    public Review(String userId, String name, int rating, String text) {
        this.userId = userId;
        this.name   = name;
        this.rating = rating;
        this.text   = text;
    }
}
