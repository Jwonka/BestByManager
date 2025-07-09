package com.example.bestbymanager.utilities;

import org.mindrot.jbcrypt.BCrypt;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final SecureRandom RND = new SecureRandom();
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SYMBOL = "!?#@$&*";

    /** 12-char random string, e.g. 6c2F9$m8Q!5v */
    public static String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(random(UPPER)).append(random(LOWER)).append(random(DIGIT)).append(random(SYMBOL));
        for (int i = 0; i < 8; i++) {
            sb.append(random(UPPER + LOWER + DIGIT + SYMBOL));
        }
        return shuffle(sb.toString());
    }

    private static char random(String pool)   { return pool.charAt(RND.nextInt(pool.length())); }
    private static String shuffle(String in)  {
        char[] out = in.toCharArray();
        for (int i = out.length - 1; i > 0; i--) {
            int j = RND.nextInt(i + 1);
            char temp = out[i];
            out[i]   = out[j];
            out[j]   = temp;
        }
        return new String(out);
    }

    /** BCrypt-hash helper */
    public static String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
}