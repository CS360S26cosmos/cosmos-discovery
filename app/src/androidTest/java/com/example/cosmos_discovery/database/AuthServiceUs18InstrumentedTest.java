package com.example.cosmos_discovery.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cosmos_discovery.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AuthServiceUs18InstrumentedTest {

    private static final int TIMEOUT_SECS = 20;
    private static final String TEST_PASSWORD = "ValidPass1!";
    private static final String TEST_NAME = "US18 Test User";

    private AuthService authService;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String createdEmail;
    private String createdUid;

    @Before
    public void setUp() {
        authService = new AuthService();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        auth.signOut();

        createdEmail = "us18_" + UUID.randomUUID().toString().replace("-", "")
                + "@lums.edu.pk";
        createdUid = null;
    }

    @After
    public void tearDown() throws Exception {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null && createdEmail != null) {
            try {
                awaitTask(auth.signInWithEmailAndPassword(createdEmail, TEST_PASSWORD));
                currentUser = auth.getCurrentUser();
            } catch (Exception ignored) {
                // If sign-in fails, cleanup may not be possible; keep teardown resilient.
            }
        }

        if (createdUid != null) {
            try {
                awaitTask(db.collection("users").document(createdUid).delete());
            } catch (Exception ignored) {
            }
        }

        if (currentUser != null) {
            try {
                awaitTask(currentUser.delete());
            } catch (Exception ignored) {
            }
        }

        auth.signOut();
    }

    @Test
    public void signUp_validUniversityEmail_createsNewUserAndFirestoreDoc() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> successUser = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();

        authService.signUp(
                createdEmail,
                TEST_PASSWORD,
                TEST_NAME,
                user -> {
                    successUser.set(user);
                    latch.countDown();
                },
                error -> {
                    failure.set(error);
                    latch.countDown();
                }
        );

        assertTrue("signUp timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("signUp failed: " + failure.get(), failure.get());
        assertNotNull("User callback should not be null", successUser.get());

        FirebaseUser current = auth.getCurrentUser();
        assertNotNull("FirebaseAuth should have a signed-in user after sign-up", current);

        createdUid = current.getUid();

        assertEquals(createdEmail, current.getEmail());

        DocumentSnapshot snapshot =
                awaitTask(db.collection("users").document(createdUid).get());

        assertTrue("User document should exist in Firestore", snapshot.exists());
        User firestoreUser = snapshot.toObject(User.class);
        assertNotNull("Firestore user should deserialize", firestoreUser);
        assertEquals(createdEmail, firestoreUser.getEmail());
        assertEquals(TEST_NAME, firestoreUser.getName());
    }

    @Test
    public void signIn_unverifiedUser_callsOnUnverified() throws Exception {
        signUpHelper();

        auth.signOut();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean unverifiedCalled = new AtomicBoolean(false);
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<String> failure = new AtomicReference<>();

        authService.signIn(
                createdEmail,
                TEST_PASSWORD,
                user -> {
                    successCalled.set(true);
                    latch.countDown();
                },
                () -> {
                    unverifiedCalled.set(true);
                    latch.countDown();
                },
                error -> {
                    failure.set(error);
                    latch.countDown();
                }
        );

        assertTrue("signIn timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertTrue("Expected unverified callback to run", unverifiedCalled.get());
        assertFalse("Success callback should not run for unverified user", successCalled.get());
        assertNull("Failure callback should not run for unverified user", failure.get());
        assertNull("User should be signed out after unverified login", auth.getCurrentUser());
    }

    @Test
    public void sendPasswordReset_existingUser_callsSuccess() throws Exception {
        signUpHelper();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<String> failure = new AtomicReference<>();

        authService.sendPasswordReset(
                createdEmail,
                () -> {
                    successCalled.set(true);
                    latch.countDown();
                },
                error -> {
                    failure.set(error);
                    latch.countDown();
                }
        );

        assertTrue("sendPasswordReset timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertTrue("Expected password reset callback success", successCalled.get());
        assertNull("Did not expect password reset failure", failure.get());
    }

    @Test
    public void signOut_clearsCurrentUser() {

        FakeAuthService auth = new FakeAuthService();

        // simulate login
        auth.signIn("test@lums.edu.pk", "password123");

        assertNotNull(auth.getCurrentUser());

        // perform logout
        auth.signOut();

        assertNull(auth.getCurrentUser());
    }

    private void signUpHelper() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> successUser = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();

        authService.signUp(
                createdEmail,
                TEST_PASSWORD,
                TEST_NAME,
                user -> {
                    successUser.set(user);
                    latch.countDown();
                },
                error -> {
                    failure.set(error);
                    latch.countDown();
                }
        );

        assertTrue("signUp helper timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        assertNull("signUp helper failed: " + failure.get(), failure.get());
        assertNotNull("signUp helper returned null user", successUser.get());

        FirebaseUser current = auth.getCurrentUser();
        assertNotNull("Expected signed-in user after helper sign-up", current);
        createdUid = current.getUid();
    }

    private static <T> T awaitTask(Task<T> task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        task.addOnSuccessListener(value -> {
            result.set(value);
            latch.countDown();
        }).addOnFailureListener(e -> {
            error.set(e);
            latch.countDown();
        });

        assertTrue("Firebase task timed out", latch.await(TIMEOUT_SECS, TimeUnit.SECONDS));

        if (error.get() != null) {
            throw error.get();
        }

        return result.get();
    }
}