package com.example.cosmos_discovery;

import com.example.cosmos_discovery.database.AuthService;
import com.example.cosmos_discovery.util.RoleUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Shared setup utilities for Espresso tests.
 */
public class EspressoTestHelper {

    private EspressoTestHelper() {}

    /**
     * Populates {@link RoleUtil} with the currently signed-in user's Firestore document,
     * mirroring what {@code SplashActivity} does during a normal app launch.
     *
     * <p>Espresso tests that call {@code ActivityScenario.launch()} directly bypass
     * {@code SplashActivity}, so {@code RoleUtil.getCurrentUser()} would otherwise be null.
     * This causes {@link com.example.cosmos_discovery.util.RsvpHandler} to reject RSVP
     * actions, {@link com.example.cosmos_discovery.ui.student.MyEventsFragment} to skip
     * attaching its Firestore listener, and the sidebar to show no user info.
     *
     * <p>Call this in {@code @Before}, before {@code ActivityScenario.launch()}.
     * No-op if {@code RoleUtil} is already populated or if no Firebase user is signed in.
     */
    public static void populateRoleUtil() throws InterruptedException {
        if (RoleUtil.getCurrentUser() != null) return;
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        CountDownLatch latch = new CountDownLatch(1);
        new AuthService().fetchUserDocument(
                firebaseUser.getUid(),
                user -> { RoleUtil.setCurrentUser(user); latch.countDown(); },
                err  -> latch.countDown()
        );
        latch.await(10, TimeUnit.SECONDS);
    }
}
