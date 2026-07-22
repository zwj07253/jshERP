package com.jsh.erp.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Passwords are currently MD5-digested by the legacy web client before transport.
 * BCrypt protects that credential at rest while legacy MD5 rows are upgraded lazily.
 */
@Service
public class UserPasswordService {

    private static final Pattern CLIENT_CREDENTIAL = Pattern.compile("^[a-fA-F0-9]{32}$");
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[ayb]\\$.*");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String encode(String clientCredential) {
        requireValidClientCredential(clientCredential);
        return encoder.encode(clientCredential.toLowerCase());
    }

    public boolean matches(String clientCredential, String storedPassword) {
        if (!isValidClientCredential(clientCredential) || storedPassword == null) {
            return false;
        }
        String normalized = clientCredential.toLowerCase();
        if (BCRYPT_HASH.matcher(storedPassword).matches()) {
            return encoder.matches(normalized, storedPassword);
        }
        return normalized.equalsIgnoreCase(storedPassword);
    }

    public boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !BCRYPT_HASH.matcher(storedPassword).matches();
    }

    public void requireValidClientCredential(String credential) {
        if (!isValidClientCredential(credential)) {
            throw new IllegalArgumentException("密码凭据格式不合法");
        }
    }

    private boolean isValidClientCredential(String credential) {
        return credential != null && CLIENT_CREDENTIAL.matcher(credential.trim()).matches();
    }
}
