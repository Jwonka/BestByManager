package com.bestbymanager.app.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public final class IdleManager {
    private static final String PREFS = "idle_prefs";
    private static final String KEY_LAST_TOUCH = "last_touch";
    private static final long IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 min

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void touch(Context c) {
        prefs(c).edit().putLong(KEY_LAST_TOUCH, System.currentTimeMillis()).apply();
    }

    public static boolean isExpired(Context c) {
        long last = prefs(c).getLong(KEY_LAST_TOUCH, 0L);
        return last > 0 && (System.currentTimeMillis() - last) > IDLE_TIMEOUT_MS;
    }

    public static void clear(Context c) {
        prefs(c).edit().remove(KEY_LAST_TOUCH).apply();
    }
}