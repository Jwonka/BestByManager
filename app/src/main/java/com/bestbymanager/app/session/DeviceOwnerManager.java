package com.bestbymanager.app.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

public final class DeviceOwnerManager {
    private DeviceOwnerManager() {}

    private static final String PREFS = "device_owner";
    private static final String KEY_OWNER_ID = "owner_employee_id";
    private static final String KEY_LOCK_AFTER_IDLE = "lock_after_idle";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void setOwnerEmployeeId(Context ctx, long employeeId) {
        prefs(ctx).edit().putLong(KEY_OWNER_ID, employeeId).apply();
    }

    public static long getOwnerEmployeeId(Context ctx) {
        return prefs(ctx).getLong(KEY_OWNER_ID, -1L);
    }

    @Nullable
    public static Long getOwnerEmployeeIdOrNull(Context ctx) {
        long id = getOwnerEmployeeId(ctx);
        return id > 0 ? id : null;
    }

    public static boolean hasOwner(Context ctx) {
        return getOwnerEmployeeId(ctx) > 0;
    }

    public static boolean isActiveEmployeeOwner(Context ctx) {
        long ownerId = getOwnerEmployeeId(ctx);
        return ownerId > 0 && ActiveEmployeeManager.getActiveEmployeeId(ctx) == ownerId;
    }

    public static void setLockAfterIdleEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_LOCK_AFTER_IDLE, enabled).apply();
    }

    public static boolean isLockAfterIdleEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_LOCK_AFTER_IDLE, false);
    }

    /** Call when ownership changes so the new owner must re-enroll recovery. */
    public static void clearOwner(Context ctx) {
        prefs(ctx).edit().remove(KEY_OWNER_ID).apply();
    }
}