package com.bestbymanager.app.session;

import android.content.Context;
import android.content.SharedPreferences;
import com.bestbymanager.app.data.entities.Employee;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import androidx.preference.PreferenceManager;

/** @noinspection unused*/
public final class Session {
    private static final Session INSTANCE = new Session();

    // Kiosk unlock session (admin). Active employee selection is managed by ActiveEmployeeManager.
    private volatile Employee currentAdmin;
    private static final String PREFS = "bestby_state";

    // Session-scoped keys (kiosk unlock + limited/reset mode)
    private static final String K_ADMIN_ID   = "admin_id";
    private static final String K_LIMITED_ID = "limited_employee_id";
    private static final String K_RESET      = "must_reset";

    // Default prefs keys
    private static final String PREF_KEY_ADMIN_ID   = "current_admin_id";
    private static final String PREF_KEY_ADMIN_NAME = "current_admin_name";
    private static final String PREF_KEY_IS_ADMIN   = "current_is_admin";
    private static final long UNKNOWN_ID = -1L;
    private final AtomicLong adminId = new AtomicLong(UNKNOWN_ID);
    private final AtomicReference<String> adminName = new AtomicReference<>("");
    private final AtomicBoolean isAdmin = new AtomicBoolean(false);

    // When unlocked: unlockedAdminId != null
    private Long unlockedAdminId = null;

    // When in limited/reset mode: limitedEmployeeId != null and mustResetPassword = true
    private Long limitedEmployeeId = null;
    private boolean mustResetPassword = false;
    private SharedPreferences sessionSP;

    private Session() {}
    public static Session get() { return INSTANCE; }

    /** Unlock kiosk: MUST be admin. Callers should enforce employee.isAdmin() before calling. */
    public synchronized void unlockKiosk(Employee employee, Context context) {
        ensureSessionSp(context);

        currentAdmin = employee;

        long id = employee.getEmployeeID();
        unlockedAdminId = id;
        limitedEmployeeId = null;
        mustResetPassword = false;

        adminId.set(id);
        adminName.set(employee.getEmployeeName());
        isAdmin.set(employee.isAdmin());

        save();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit()
                .putLong(PREF_KEY_ADMIN_ID, id)
                .putString(PREF_KEY_ADMIN_NAME, employee.getEmployeeName())
                .putBoolean(PREF_KEY_IS_ADMIN, employee.isAdmin())
                .apply();
    }

    /** Lock kiosk: clears admin unlock session + limited/reset state. */
    public synchronized void lockKiosk(Context context) {
        Context app = context.getApplicationContext();

        // clear in-memory
        currentAdmin = null;
        unlockedAdminId = null;
        limitedEmployeeId = null;
        mustResetPassword = false;

        adminId.set(UNKNOWN_ID);
        adminName.set("");
        isAdmin.set(false);

        // clear bestby_session
        ensureSessionSp(app);
        sessionSP.edit().clear().apply();

        // clear default prefs (current_admin_*)
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(app);
        sp.edit()
                .remove(PREF_KEY_ADMIN_ID)
                .remove(PREF_KEY_ADMIN_NAME)
                .remove(PREF_KEY_IS_ADMIN)
                .apply();
    }

    // ---- Kiosk unlock getters ----
    public long adminId() { return adminId.get(); }

    public String adminName() { return adminName.get(); }

    public boolean isAdmin() { return isAdmin.get(); }

    public boolean isUnlocked() { return unlockedAdminId != null; }

    // ---- Limited/reset mode ----
    // Used when forcing a password reset flow for a specific employee id.
    public synchronized void startLimited(long employeeIdNeedingReset, Context context) {
        ensureSessionSp(context);

        // Do NOT unlock kiosk here.
        currentAdmin = null;
        unlockedAdminId = null;

        limitedEmployeeId = (employeeIdNeedingReset > 0) ? employeeIdNeedingReset : null;
        mustResetPassword = true;

        // Clear admin identity in memory/default prefs
        adminId.set(UNKNOWN_ID);
        adminName.set("");
        isAdmin.set(false);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit()
                .remove(PREF_KEY_ADMIN_ID)
                .remove(PREF_KEY_ADMIN_NAME)
                .remove(PREF_KEY_IS_ADMIN)
                .apply();

        save();
    }

    public boolean requiresPasswordReset() { return mustResetPassword; }

    public synchronized void clearResetRequirement() {
        mustResetPassword = false;
        limitedEmployeeId = null;
        save();
    }

    /** Employee id that must reset (only valid when requiresPasswordReset()==true). */
    public Long limitedEmployeeId() { return limitedEmployeeId; }

    // ---- Persistence ----
    public void preload(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        adminId.set(sp.getLong(PREF_KEY_ADMIN_ID, UNKNOWN_ID));
        adminName.set(sp.getString(PREF_KEY_ADMIN_NAME, ""));
        isAdmin.set(sp.getBoolean(PREF_KEY_IS_ADMIN, false));

        ensureSessionSp(context);
        load();
    }

    private void load() {
        if (sessionSP == null) return;

        long id = sessionSP.getLong(K_ADMIN_ID, UNKNOWN_ID);
        unlockedAdminId = (id > 0) ? id : null;

        long limited = sessionSP.getLong(K_LIMITED_ID, UNKNOWN_ID);
        limitedEmployeeId = (limited > 0) ? limited : null;

        mustResetPassword = sessionSP.getBoolean(K_RESET, false);

        // If in limited/reset mode, ensure kiosk is not considered unlocked.
        if (mustResetPassword) unlockedAdminId = null;
    }

    private void save() {
        if (sessionSP == null) return;

        SharedPreferences.Editor e = sessionSP.edit();

        if (unlockedAdminId != null) e.putLong(K_ADMIN_ID, unlockedAdminId);
        else e.remove(K_ADMIN_ID);

        if (limitedEmployeeId != null) e.putLong(K_LIMITED_ID, limitedEmployeeId);
        else e.remove(K_LIMITED_ID);

        e.putBoolean(K_RESET, mustResetPassword);
        e.apply();
    }

    private void ensureSessionSp(Context context) {
        if (sessionSP == null) {
            sessionSP = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }
}