package com.example.cosmos_discovery.database;

import android.util.Log;

import com.example.cosmos_discovery.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.function.Consumer;

/**
 * Provides authentication-related operations backed by Firebase Authentication
 * and Cloud Firestore.
 */
public class AuthService {
    private static final String TAG = "AuthService";
    private static final String USERS_COLLECTION = "users";

    private static final String ALLOWED_DOMAIN = "@lums.edu.pk";
    private static final String ALLOWED_PASSWORD_SPECIALS = "!@#$%^&*!?~";

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    /**
     * Default constructor used by the Android app.
     */
    public AuthService() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }
    AuthService(FirebaseAuth auth, FirebaseFirestore db) {
        mAuth = auth;
        mDb   = db;
    }


    /**
     * Returns true if the email ends with the allowed university domain.
     *
     * @param email The email address to validate.
     * @return True if the email belongs to the allowed domain; otherwise false.
     */
    public boolean isUniversityEmail(String email) {
        return email != null && email.trim().toLowerCase().endsWith(ALLOWED_DOMAIN);
    }

    /**
     * Returns true if the password is strong.
     * Requirements:
     * - At least 8 characters
     * - At least one up    percase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character from: {@literal !@#$%^&*!?~}
     * - No whitespace characters
     * - Disallows {@literal ._()-+=[]{}:;/\|} and any unsupported symbols
     *
     * @param password The password to validate.
     * @return True when the password satisfies all strength requirements.
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int index = 0; index < password.length(); index++) {
            char currentChar = password.charAt(index);

            if (Character.isWhitespace(currentChar)) {
                return false;
            }

            if (Character.isUpperCase(currentChar)) {
                hasUpper = true;
            } else if (Character.isLowerCase(currentChar)) {
                hasLower = true;
            } else if (Character.isDigit(currentChar)) {
                hasDigit = true;
            } else {
                if (ALLOWED_PASSWORD_SPECIALS.indexOf(currentChar) >= 0) {
                    hasSpecial = true;
                } else {
                    return false;
                }
            }
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    // ── Sign Up ───────────────────────────────────────────────────────────

    /**
     * Registers a new user with Firebase Auth, sends a verification email,
     * and writes the user document to Firestore.
     *
     * @param email        Must be a university email address.
     * @param password     Plain-text password (Firebase handles hashing).
     * @param displayName  User's full name.
     * @param onSuccess    Called with the new User object on success.
     * @param onFailure    Called with an error message string on failure.
     */
    public void signUp(String email, String password, String displayName,
                       Consumer<User> onSuccess, Consumer<String> onFailure) {

        if (!isUniversityEmail(email)) {
            onFailure.accept("Please use your university email address ("
                    + ALLOWED_DOMAIN + ").");
            return;
        }
        if (!isValidPassword(password)) {
            onFailure.accept("Password must follow minimum strength requirements.");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        onFailure.accept("Sign up failed. Please try again.");
                        return;
                    }

                    // Send email verification immediately after account creation
                    firebaseUser.sendEmailVerification()
                            .addOnFailureListener(e ->
                                    Log.w(TAG, "Verification email failed: " + e.getMessage()));

                    // Create the Firestore user document
                    User newUser = new User(
                            firebaseUser.getUid(), email.trim(), displayName.trim());

                    mDb.collection(USERS_COLLECTION)
                            .document(firebaseUser.getUid())
                            .set(newUser)
                            .addOnSuccessListener(unused -> onSuccess.accept(newUser))
                            .addOnFailureListener(e -> {
                                // Auth account was created but Firestore write failed.
                                // Delete the auth account to keep state consistent.
                                firebaseUser.delete();
                                onFailure.accept("Account setup failed. Please try again.");
                            });
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "Sign up failed.";
                    if (msg.contains("email address is already in use")) {
                        msg = "This email is already registered. Please sign in.";
                    }
                    onFailure.accept(msg);
                });
    }
    // ── Sign In ───────────────────────────────────────────────────────────

    /**
     * Signs the user in. Checks that the email is verified and the account
     * is active in Firestore before allowing access.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @param onSuccess Called with the User object (including role) on success.
     * @param onUnverified Called if the email has not been verified yet.
     * @param onFailure    Called with an error message on any other failure.
     */
    public void signIn(String email, String password,
                       Consumer<User> onSuccess,
                       Runnable onUnverified,
                       Consumer<String> onFailure) {

        mAuth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        onFailure.accept("Sign in failed.");
                        return;
                    }

                    // Gate on email verification
                    if (!firebaseUser.isEmailVerified()) {
                        mAuth.signOut(); // don't leave them signed in
                        onUnverified.run();
                        return;
                    }

                    // Fetch the Firestore user doc to get role and active status
                    fetchUserDocument(firebaseUser.getUid(), onSuccess, onFailure);
                })
                .addOnFailureListener(e -> {
                    String msg = "Incorrect email or password.";
                    onFailure.accept(msg);
                });
    }
    // ── Fetch User Document ───────────────────────────────────────────────

    /**
     * Reads the user's Firestore document by UID.
     * Used after login to get the role and account status.
     *
     * @param uid The Firebase user id.
     * @param onSuccess Called with the mapped User object.
     * @param onFailure Called with an error message if lookup fails.
     */
    public void fetchUserDocument(String uid,
                                  Consumer<User> onSuccess,
                                  Consumer<String> onFailure) {
        mDb.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        onFailure.accept("User profile not found. Please contact support.");
                        return;
                    }
                    User user = snapshot.toObject(User.class);
                    if (user == null) {
                        onFailure.accept("Failed to load profile.");
                        return;
                    }
                    if (!user.isActive()) {
                        mAuth.signOut();
                        onFailure.accept("Your account has been deactivated. "
                                + "Please contact an administrator.");
                        return;
                    }
                    onSuccess.accept(user);
                })
                .addOnFailureListener(e ->
                        onFailure.accept("Could not load profile. Check your connection."));
    }

    // ── Other Auth Operations ─────────────────────────────────────────────

    /**
     * Sends a password reset email to the given address.
     *
     * @param email The target email address.
     * @param onSuccess Called when the reset email is sent.
     * @param onFailure Called with an error message if sending fails.
     */
    public void sendPasswordReset(String email,
                                  Runnable onSuccess,
                                  Consumer<String> onFailure) {
        mAuth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept(
                        "Could not send reset email. Check the address and try again."));
    }


    /**
     * Re-sends the verification email to the currently signed-in user.
     *
     * @param onSuccess Called when resend succeeds.
     * @param onFailure Called with an error message if resend fails.
     */
    public void resendVerificationEmail(Runnable onSuccess, Consumer<String> onFailure) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { onFailure.accept("Not signed in."); return; }
        user.sendEmailVerification()
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> onFailure.accept("Could not resend. Try again later."));
    }

    /** Signs out the current user. */
    public void signOut() {
        mAuth.signOut();
    }

    /**
     * Returns the currently signed-in Firebase user, or null.
     *
     * @return The signed-in Firebase user, or null if no user is signed in.
     */
    public FirebaseUser getCurrentFirebaseUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Returns true if there is a signed-in, verified user.
     *
     * @return True when a verified user is signed in; otherwise false.
     */
    public boolean isSignedIn() {
        FirebaseUser u = mAuth.getCurrentUser();
        return u != null && u.isEmailVerified();
    }
}
