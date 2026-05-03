package com.example.cosmos_discovery.database;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthServiceTest {

    private AuthService authService;

    @Before
    public void setUp() {
        // Safe because these unit tests only call pure validation methods.
        authService = new AuthService(null, null);
    }

    @Test
    public void isUniversityEmail_validLumsEmail_returnsTrue() {
        assertTrue(authService.isUniversityEmail("student@lums.edu.pk"));
    }

    @Test
    public void isUniversityEmail_validLumsEmailWithSpacesAndCaps_returnsTrue() {
        assertTrue(authService.isUniversityEmail("  STUDENT@LUMS.EDU.PK  "));
    }

    @Test
    public void isUniversityEmail_nonLumsEmail_returnsFalse() {
        assertFalse(authService.isUniversityEmail("student@gmail.com"));
    }

    @Test
    public void isUniversityEmail_nullEmail_returnsFalse() {
        assertFalse(authService.isUniversityEmail(null));
    }

    @Test
    public void isValidPassword_validPassword_returnsTrue() {
        assertTrue(authService.isValidPassword("ValidPass1!"));
    }

    @Test
    public void isValidPassword_tooShort_returnsFalse() {
        assertFalse(authService.isValidPassword("Aa1!aaa"));
    }

    @Test
    public void isValidPassword_missingUppercase_returnsFalse() {
        assertFalse(authService.isValidPassword("validpass1!"));
    }

    @Test
    public void isValidPassword_missingLowercase_returnsFalse() {
        assertFalse(authService.isValidPassword("VALIDPASS1!"));
    }

    @Test
    public void isValidPassword_missingDigit_returnsFalse() {
        assertFalse(authService.isValidPassword("ValidPass!"));
    }

    @Test
    public void isValidPassword_missingSpecial_returnsFalse() {
        assertFalse(authService.isValidPassword("ValidPass1"));
    }

    @Test
    public void isValidPassword_hasWhitespace_returnsFalse() {
        assertFalse(authService.isValidPassword("Valid Pass1!"));
    }

    @Test
    public void isValidPassword_hasUnsupportedSpecial_returnsFalse() {
        assertFalse(authService.isValidPassword("ValidPass1."));
    }

    @Test
    public void isValidPassword_null_returnsFalse() {
        assertFalse(authService.isValidPassword(null));
    }
}