package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.User;

public class FakeAuthService {

    private User currentUser;

    public void signIn(String email, String password) {
        // Simulate successful login
        currentUser = new User();
        currentUser.setEmail(email);
        currentUser.setName("Test User");
        currentUser.setRole("student");
        currentUser.setActive(true);
    }

    public void signOut() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}