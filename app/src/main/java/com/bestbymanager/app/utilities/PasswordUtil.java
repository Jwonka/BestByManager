package com.bestbymanager.app.utilities;

import org.mindrot.jbcrypt.BCrypt;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final SecureRandom RND = new SecureRandom();

    // Excludes visually confusing chars (0,O,l,1)
    private static final String ALPHANUM =
            "ABCDEFGHJKLMNPQRSTUVWXYZ" +
                    "abcdefghijkmnopqrstuvwxyz" +
                    "23456789";

    private static final int TEMP_LENGTH = 14;

    /** 14-char secure random temporary password */
    public static String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_LENGTH);
        for (int i = 0; i < TEMP_LENGTH; i++) {
            sb.append(ALPHANUM.charAt(RND.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    /** BCrypt-hash helper */
    public static String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
}