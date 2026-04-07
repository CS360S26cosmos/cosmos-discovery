package com.example.cosmos_discovery.database;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cosmos_discovery.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Integration tests for AuthService.
 *
 * These tests require the app to already be signed in before running.
 * Sign in to the app normally, then run these tests.
 *
 * HOW TO RUN:
 * - Sign in to the app on the device/emulator first.
 * - Connect a physical device via USB (with USB Debugging on), or start an emulator
 * - Right-click this file in Android Studio → Run 'AuthServiceIntegrationTest'
 *
 * NOTE: These are NOT run in GitHub Actions CI. They run locally only.
 * CI runs the plain unit tests in src/test/ via ./gradlew test.
 */
@RunWith(AndroidJUnit4.class)
public class AuthServiceIntegrationTest {

    // ── Constants ─────────────────────────────────────────────────────────────
        /** Maximum wait time (seconds) for async Firebase callbacks. */
    private static final int TIMEOUT_SECS = 15;

    // ── Fields ────────────────────────────────────────────────────────────────
        /** Service under test. */
    private AuthService       mAuthService;
        /** Firebase Authentication client used in assertions and setup. */
    private FirebaseAuth      mAuth;
        /** Firestore client used for direct document verification. */
    private FirebaseFirestore mDb;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @Before
    public void setUp() {
        mAuthService = new AuthService();
        mAuth        = FirebaseAuth.getInstance();
        mDb          = FirebaseFirestore.getInstance();
    }

    // ── Test 7 ────────────────────────────────────────────────────────────────

    /**
     * Fetches the Firestore user document for the currently signed-in account.
     * Requires the app to already be signed in.
     *
     * Verifies the document is fetched successfully and the user object is complete.
     */
    @Test
    public void fetchUserDocument_existingAccount_returnsCompleteUser()
            throws InterruptedException {

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        assertNotNull("No signed-in user — sign in to the app before running this test",
                firebaseUser);
        String uid = firebaseUser.getUid();

        // Fetch the document
        CountDownLatch fetchLatch         = new CountDownLatch(1);
        AtomicReference<User> fetched    = new AtomicReference<>();
        AtomicReference<String> fetchErr = new AtomicReference<>();

        mAuthService.fetchUserDocument(uid,
                user -> { fetched.set(user); fetchLatch.countDown(); },
                err  -> { fetchErr.set(err); fetchLatch.countDown(); }
        );

        assertTrue("fetchUserDocument timed out", fetchLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("fetchUserDocument failed: " + fetchErr.get(), fetchErr.get());

        User u = fetched.get();
        assertNotNull("fetchUserDocument returned null", u);
        assertEquals("uid mismatch", uid, u.getUid());
        assertNotNull("email should not be null", u.getEmail());
        assertNotNull("name should not be null", u.getName());
        assertTrue("isActive should be true", u.isActive());
    }

    // ── Test 8 ────────────────────────────────────────────────────────────────

    /**
     * Calls fetchUserDocument() with a UID that does not exist in Firestore.
     * Verifies the error callback fires rather than returning null silently.
     */
    @Test
    public void fetchUserDocument_nonExistentUid_callsErrorCallback()
            throws InterruptedException {

        CountDownLatch latch          = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();

        mAuthService.fetchUserDocument("uid-that-does-not-exist-xyz",
                user -> latch.countDown(),           // should NOT be called
                err  -> { error.set(err); latch.countDown(); }
        );

        assertTrue("fetchUserDocument timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNotNull("Expected an error for non-existent UID but got none", error.get());
    }
}
