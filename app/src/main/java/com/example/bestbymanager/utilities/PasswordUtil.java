package com.example.bestbymanager.utilities;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

public final class PasswordUtil {
    private static final SecureRandom RND = new SecureRandom();
    private static final String ALPHANUM = "ABcDeFgHJKLMnPQRSTuVWxYz23456789!?#@$&*";

    /** 8-char random string, e.g. 6C2F9M8Q */
    public static String generateTemp() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHANUM.charAt(RND.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    /** BCrypt-hash helper */
    public static String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
}