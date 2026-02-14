package com.bestbymanager.app.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

/** Pref-backed "active employee" selection for kiosk mode. */
public final class ActiveEmployeeManager {

    private static final String PREFS = "bestby_session";
    private static final String K_ACTIVE_EMPLOYEE_ID = "activeEmployeeId";
    private static final long UNKNOWN_ID = -1L;

    private ActiveEmployeeManager() {}

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static long getActiveEmployeeId(Context context) {
        return sp(context).getLong(K_ACTIVE_EMPLOYEE_ID, UNKNOWN_ID);
    }

    public static boolean hasActiveEmployee(Context context) {
        return getActiveEmployeeId(context) > 0;
    }

    public static void setActiveEmployeeId(Context context, long userId) {
        if (userId <= 0) throw new IllegalArgumentException("userId must be > 0");
        sp(context).edit().putLong(K_ACTIVE_EMPLOYEE_ID, userId).apply();
    }

    public static void clearActiveEmployee(Context context) {
        sp(context).edit().remove(K_ACTIVE_EMPLOYEE_ID).apply();
    }

    /** Convenience for null semantics in callers. */
    @Nullable
    public static Long getActiveEmployeeIdOrNull(Context context) {
        long id = getActiveEmployeeId(context);
        return id > 0 ? id : null;
    }
}
