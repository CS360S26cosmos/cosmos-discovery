package com.example.cosmos_discovery.database;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cosmos_discovery.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
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
 * These tests write real data to Firebase Auth and Firestore,
 * verify the data is correct, then delete everything after each test.
 *
 * HOW TO RUN:
 * - Connect a physical device via USB (with USB Debugging on), or start an emulator
 * - Right-click this file in Android Studio → Run 'AuthServiceIntegrationTest'
 * - Watch Firebase Console → Auth + Firestore to see data appear and disappear live
 *
 * NOTE: These are NOT run in GitHub Actions CI. They run locally only.
 * CI runs the plain unit tests in src/test/ via ./gradlew test.
 */
@RunWith(AndroidJUnit4.class)
public class AuthServiceIntegrationTest {

    // ── Constants ─────────────────────────────────────────────────────────────
        /** Test email used for integration scenarios. */
    private static final String TEST_EMAIL    = "27100026@lums.edu.pk";
        /** Test password used for integration scenarios. */
    private static final String TEST_PASSWORD = "TestPass@123";
        /** Display name used for test user creation. */
    private static final String TEST_NAME     = "Integration Test User";
        /** Maximum wait time (seconds) for async Firebase callbacks. */
    private static final int    TIMEOUT_SECS  = 15;

    // ── Fields ────────────────────────────────────────────────────────────────
        /** Service under test. */
    private AuthService       mAuthService;
        /** Firebase Authentication client used in assertions and setup. */
    private FirebaseAuth      mAuth;
        /** Firestore client used for direct document verification. */
    private FirebaseFirestore mDb;

    // ── Setup & Teardown ──────────────────────────────────────────────────────

        /**
         * Initializes Firebase clients and resets authentication state before each test.
         *
         * @throws InterruptedException If setup waits are interrupted.
         */
    @Before
    public void setUp() throws InterruptedException {
        mAuthService = new AuthService();
        mAuth        = FirebaseAuth.getInstance();
        mDb          = FirebaseFirestore.getInstance();

        // Always start from a signed-out, clean state
        mAuth.signOut();
        // cleanUpTestAccount();
    }

//     @After
//     public void tearDown() throws InterruptedException {
//         // Delete test data from Firebase regardless of whether the test passed or failed
//         cleanUpTestAccount();
//         mAuth.signOut();
//     }

    // ── Test 1 ────────────────────────────────────────────────────────────────

    /**
     * Signs up a new user and verifies:
     * - The success callback fires and returns a User object
     * - A real Firebase Auth account was created with the correct email
     */
    @Test
    public void signUp_createsAuthAccountAndFirestoreDocument()
            throws InterruptedException {

        CountDownLatch latch          = new CountDownLatch(1);
        AtomicReference<User> result  = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        mAuthService.signUp(TEST_EMAIL, TEST_PASSWORD, TEST_NAME,
                user -> { result.set(user); latch.countDown(); },
                err  -> { error.set(err);  latch.countDown(); }
        );

        assertTrue("signUp timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("signUp returned an error: " + error.get(), error.get());
        assertNotNull("signUp callback returned null user", result.get());

        // Confirm Firebase Auth account actually exists
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        assertNotNull("Firebase Auth account was not created", firebaseUser);
        assertEquals("Auth email does not match", TEST_EMAIL, firebaseUser.getEmail());
    }

    // ── Test 2 ────────────────────────────────────────────────────────────────

    /**
     * Signs up a new user then reads the Firestore document directly.
     * Verifies every field in the document matches what was passed to signUp()
     * and that all default values (role=student, isActive=true) are set correctly.
     *
     * This is the most important test — it confirms your User.java field names
     * match your Firestore document structure exactly.
     */
    @Test
    public void signUp_firestoreDocumentHasCorrectFields()
            throws InterruptedException {

        // Step 1 — Sign up
        CountDownLatch signUpLatch         = new CountDownLatch(1);
        AtomicReference<String> signUpErr  = new AtomicReference<>();

        mAuthService.signUp(TEST_EMAIL, TEST_PASSWORD, TEST_NAME,
                u   -> signUpLatch.countDown(),
                err -> { signUpErr.set(err); signUpLatch.countDown(); }
        );

        assertTrue("signUp timed out", signUpLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("signUp failed: " + signUpErr.get(), signUpErr.get());

        // Step 2 — Read the Firestore document directly (bypass AuthService)
        String uid = mAuth.getCurrentUser().getUid();

        CountDownLatch readLatch               = new CountDownLatch(1);
        AtomicReference<User> firestoreUser   = new AtomicReference<>();
        AtomicReference<String> readErr       = new AtomicReference<>();

        mDb.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    firestoreUser.set(snapshot.toObject(User.class));
                    readLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    readErr.set(e.getMessage());
                    readLatch.countDown();
                });

        assertTrue("Firestore read timed out", readLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("Firestore read failed: " + readErr.get(), readErr.get());

        // Step 3 — Verify every field
        User u = firestoreUser.get();
        assertNotNull(
                "Firestore returned null — User.java is probably missing its empty constructor",
                u
        );

        assertEquals("uid field mismatch",            uid,          u.getUid());
        assertEquals("email field mismatch",          TEST_EMAIL,   u.getEmail());
        assertEquals("displayName field mismatch",    TEST_NAME,    u.getName());
        assertEquals("role should default to student","student",    u.getRole());
        assertTrue(  "isActive should default to true",             u.isActive());
        assertTrue(  "createdAt should be a positive timestamp",    u.getCreatedAt() > 0);

        // Fields not set during signup should be null
        assertNull("batch should be null after signup",   u.getBatch());
        assertNull("major should be null after signup",   u.getMajor());
        assertNull("bio should be null after signup",     u.getBio());
    }

    // ── Test 3 ────────────────────────────────────────────────────────────────

    /**
     * Attempts to sign up with a Gmail address.
     * Verifies the error callback fires and no Firebase account is created.
     */
    @Test
    public void signUp_nonUniversityEmail_callsErrorAndCreatesNoAccount()
            throws InterruptedException {

        CountDownLatch latch          = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();

        mAuthService.signUp(
                "someone@gmail.com", TEST_PASSWORD, TEST_NAME,
                user -> latch.countDown(),              // should NOT be called
                err  -> { error.set(err); latch.countDown(); }
        );

        assertTrue("signUp timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNotNull("Expected an error for non-university email but got none", error.get());
        assertNull("No Auth account should have been created", mAuth.getCurrentUser());
    }

    // ── Test 4 ────────────────────────────────────────────────────────────────

    /**
     * Signs up twice with the same email.
     * Verifies the second attempt calls the error callback with a meaningful message,
     * not a crash or silent failure.
     */
    @Test
    public void signUp_duplicateEmail_returnsError()
            throws InterruptedException {

        // First signup — should succeed
        CountDownLatch first = new CountDownLatch(1);
        mAuthService.signUp(TEST_EMAIL, TEST_PASSWORD, TEST_NAME,
                u -> first.countDown(), e -> first.countDown());
        assertTrue("First signUp timed out", first.await(TIMEOUT_SECS, TimeUnit.SECONDS));

        mAuth.signOut();

        // Second signup — should fail
        CountDownLatch second         = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();

        mAuthService.signUp(TEST_EMAIL, TEST_PASSWORD, TEST_NAME,
                u   -> second.countDown(),
                err -> { error.set(err); second.countDown(); }
        );

        assertTrue("Second signUp timed out", second.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNotNull("Expected an error for duplicate email but got none", error.get());
    }

    // ── Test 5 ────────────────────────────────────────────────────────────────

    /**
     * Signs in with a pre-existing, already-verified account.
     * Assumes the account was created and email-verified beforehand
     * (e.g. manually via Firebase Console or a previous test run).
     *
     * Verifies the returned User object has the correct role and active status.
     */
    @Test
    public void signIn_existingVerifiedAccount_returnsCorrectUserWithRole()
            throws InterruptedException {

        mAuth.signOut();

        CountDownLatch signInLatch         = new CountDownLatch(1);
        AtomicReference<User> signedIn    = new AtomicReference<>();
        AtomicReference<String> signInErr = new AtomicReference<>();

        mAuthService.signIn(TEST_EMAIL, TEST_PASSWORD,
                user -> { signedIn.set(user); signInLatch.countDown(); },
                ()   -> { signInErr.set("email_not_verified"); signInLatch.countDown(); },
                err  -> { signInErr.set(err); signInLatch.countDown(); }
        );

        assertTrue("signIn timed out", signInLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("signIn failed: " + signInErr.get(), signInErr.get());

        User u = signedIn.get();
        assertNotNull("signIn returned null user", u);
        assertEquals("email mismatch",   TEST_EMAIL, u.getEmail());
        assertEquals("name mismatch",    TEST_NAME,  u.getName());
        assertEquals("role should be student", "student", u.getRole());
        assertTrue("isActive should be true", u.isActive());
    }

    // ── Test 6 ────────────────────────────────────────────────────────────────

    /**
     * Attempts to sign in with the wrong password on a pre-existing account.
     * Verifies the error callback fires and no user is left signed in.
     */
    @Test
    public void signIn_wrongPassword_callsErrorCallback()
            throws InterruptedException {

        mAuth.signOut();

        CountDownLatch latch          = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();

        mAuthService.signIn(TEST_EMAIL, "wrongpassword99",
                u  -> latch.countDown(),
                () -> latch.countDown(),
                err -> { error.set(err); latch.countDown(); }
        );

        assertTrue("signIn timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNotNull("Expected an error for wrong password but got none", error.get());
        assertNull("No user should be signed in after failed attempt", mAuth.getCurrentUser());
    }

    // ── Test 7 ────────────────────────────────────────────────────────────────

    /**
     * Fetches the Firestore user document for a pre-existing account.
     * Assumes the account and its Firestore document already exist.
     *
     * Verifies the document is fetched successfully and the user object is complete.
     */
    @Test
    public void fetchUserDocument_existingAccount_returnsCompleteUser()
            throws InterruptedException {

        // Sign in to get the UID for the existing account
        CountDownLatch signInLatch = new CountDownLatch(1);
        mAuth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
                .addOnCompleteListener(task -> signInLatch.countDown());
        assertTrue("signIn timed out", signInLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS));

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        assertNotNull("Test account does not exist — create it first", firebaseUser);
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
        assertEquals("uid mismatch",   uid,        u.getUid());
        assertEquals("email mismatch", TEST_EMAIL, u.getEmail());
        assertEquals("name mismatch",  TEST_NAME,  u.getName());
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

    // ── Cleanup Helper ────────────────────────────────────────────────────────

    /**
     * Deletes the test Firebase Auth account and its Firestore document.
     * Called in @Before to remove leftover data from failed previous runs,
     * and in @After to clean up after each test.
        *
        * @throws InterruptedException If any cleanup wait is interrupted.
     */
    private void cleanUpTestAccount() throws InterruptedException {

        // Sign in to the test account so we have permission to delete it
        CountDownLatch signInLatch = new CountDownLatch(1);
        mAuth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
                .addOnCompleteListener(task -> signInLatch.countDown());
        signInLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // Account does not exist — nothing to clean up
            return;
        }

        String uid = user.getUid();

        // Delete the Firestore document
        CountDownLatch firestoreLatch = new CountDownLatch(1);
        mDb.collection("users").document(uid)
                .delete()
                .addOnCompleteListener(task -> firestoreLatch.countDown());
        firestoreLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS);

        // Delete the Firebase Auth account
        CountDownLatch authLatch = new CountDownLatch(1);
        user.delete()
                .addOnCompleteListener(task -> authLatch.countDown());
        authLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS);

        mAuth.signOut();
    }
}