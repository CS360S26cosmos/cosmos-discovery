package com.example.cosmos_discovery.database;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AdminServiceTest {

    /**
     * updateUserRole with "admin" as newRole must reject via onFailure
     * without touching Firestore (so null db is safe here).
     */
    @Test
    public void updateUserRole_adminNewRole_callsOnFailure() {
        AdminService service = new AdminService(null);

        boolean[] successCalled = {false};
        String[]  errorMessage  = {null};

        service.updateUserRole(
                "someUserId",
                "admin",
                () -> successCalled[0] = true,
                err -> errorMessage[0] = err);

        assertFalse("onSuccess must not be called for admin role", successCalled[0]);
        assertNotNull("onFailure must be called with an error message", errorMessage[0]);
    }

    @Test
    public void updateUserRole_adminNewRole_errorMessageNotEmpty() {
        AdminService service = new AdminService(null);

        String[] errorMessage = {null};

        service.updateUserRole(
                "someUserId",
                "admin",
                () -> {},
                err -> errorMessage[0] = err);

        assertNotNull(errorMessage[0]);
        assertFalse("Error message must not be empty", errorMessage[0].isEmpty());
    }
}
