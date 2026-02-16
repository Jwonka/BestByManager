package com.bestbymanager.app.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

/** Pref-backed "active employee" selection for kiosk mode. */
public final class ActiveEmployeeManager {
    private ActiveEmployeeManager() {}
    private static final String PREFS = "active_employee";
    private static final String KEY_ID = "active_employee_id";
    private static final String KEY_IS_ADMIN = "active_employee_is_admin";
    public static final long UNKNOWN_ID = -1L;
    private static SharedPreferences prefs(Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static void setActiveEmployeeId(Context ctx, long employeeId) { prefs(ctx).edit().putLong(KEY_ID, employeeId).apply(); }

    public static long getActiveEmployeeId(Context ctx) { return prefs(ctx).getLong(KEY_ID, UNKNOWN_ID); }

    public static boolean hasActiveEmployee(Context ctx) { return getActiveEmployeeId(ctx) > 0; }

    public static void setActiveEmployeeIsAdmin(Context ctx, boolean isAdmin) { prefs(ctx).edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply(); }

    public static boolean isActiveEmployeeAdmin(Context ctx) { return prefs(ctx).getBoolean(KEY_IS_ADMIN, false); }

    public static void clearActiveEmployee(Context ctx) {
        prefs(ctx).edit()
                .remove(KEY_ID)
                .remove(KEY_IS_ADMIN)
                .apply();
    }

    /** Convenience for null semantics in callers. */
    @Nullable
    public static Long getActiveEmployeeIdOrNull(Context context) {
        long id = getActiveEmployeeId(context);
        return id > 0 ? id : null;
    }
}
