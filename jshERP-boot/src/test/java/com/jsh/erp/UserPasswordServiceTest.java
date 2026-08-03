package com.jsh.erp;

import com.jsh.erp.service.UserPasswordService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPasswordServiceTest {

    private final UserPasswordService passwordService = new UserPasswordService();

    @Test
    void bcryptProtectsClientCredentialAtRest() {
        String credential = "e10adc3949ba59abbe56e057f20f883e";
        String encoded = passwordService.encode(credential);

        assertNotEquals(credential, encoded);
        assertTrue(encoded.startsWith("$2"));
        assertTrue(passwordService.matches(credential, encoded));
        assertFalse(passwordService.matches("00000000000000000000000000000000", encoded));
    }

    @Test
    void legacyMd5RowsRemainUsableAndNeedUpgrade() {
        String credential = "e10adc3949ba59abbe56e057f20f883e";

        assertTrue(passwordService.matches(credential, credential));
        assertTrue(passwordService.needsUpgrade(credential));
    }

    @Test
    void rejectsMalformedCredentials() {
        assertThrows(IllegalArgumentException.class, () -> passwordService.encode("123456"));
        assertFalse(passwordService.matches("123456", "123456"));
    }
}
