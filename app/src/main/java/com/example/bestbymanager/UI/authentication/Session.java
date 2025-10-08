package com.example.bestbymanager.UI.authentication;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.bestbymanager.data.entities.User;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import androidx.preference.PreferenceManager;

/** @noinspection unused*/
public class Session {
    private static final Session INSTANCE = new Session();
    private volatile User current;
    private static final String PREFS = "bestby_session";
    private static final String K_USER = "userId";
    private static final String K_RESET = "mustReset";
    private static final String PREF_KEY_ID   = "current_user_id";
    private static final String PREF_KEY_NAME = "current_user_name";
    private static final String PREF_KEY_ADM  = "current_user_admin";
    private static final long   UNKNOWN_ID    = 1L;
    private final AtomicLong uid = new AtomicLong(UNKNOWN_ID);
    private final AtomicReference<String> userName = new AtomicReference<>("");
    private final AtomicBoolean admin = new AtomicBoolean(false);
    private Long userId;
    private boolean mustResetPassword;
    private SharedPreferences sessionSP;
    private Session() {}
    public static Session get() { return INSTANCE; }
    public synchronized void logIn(User user, Context context) {
        ensureSessionSp(context);
        current = user;
        uid.set(user.getUserID());
        this.userId = user.getUserID();
        this.mustResetPassword = false;
        save();
        userName.set(user.getUserName());
        admin.set(user.isAdmin);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_KEY_ID, user.getUserID())
                 .putString(PREF_KEY_NAME, user.getUserName())
                 .putBoolean(PREF_KEY_ADM, user.isAdmin())
                 .apply();
    }

    /** @noinspection unused*/
    public synchronized void logOut(Context context) {
        ensureSessionSp(context);
        current = null;
        this.userId = null;
        this.mustResetPassword = false;
        save();
    }
    public long currentUserID() { return uid.get(); }
    public boolean isLoggedOut() { return current == null; }
    public String currentUserName() { return userName.get(); }
    public boolean currentUserIsAdmin() { return admin.get(); }

    public void startLimited(long userIdNeedingReset) {
        this.userId = userIdNeedingReset;
        this.mustResetPassword = true;
        save();
    }

    public void preload(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        uid.set(sp.getLong   (PREF_KEY_ID,   UNKNOWN_ID));
        userName.set(sp.getString(PREF_KEY_NAME, ""));
        admin.set(sp.getBoolean(PREF_KEY_ADM, false));
        ensureSessionSp(context);
        load();
    }

    private void load() {
        if (sessionSP == null) return;
        if (sessionSP.contains(K_USER)) {
            long v = sessionSP.getLong(K_USER, -1L);
            this.userId = (v >= 0) ? v : null;
        } else {
            this.userId = null;
        }
        this.mustResetPassword = sessionSP.getBoolean(K_RESET, false);
    }

    private void save() {
        if (sessionSP == null) return;
        SharedPreferences.Editor editor = sessionSP.edit();
        if (userId != null) editor.putLong(K_USER, userId); else editor.remove(K_USER);
        editor.putBoolean(K_RESET, mustResetPassword);
        editor.apply();
    }

    public boolean isLoggedIn() { return userId != null; }

    public boolean requiresPasswordReset() { return mustResetPassword; }

    public void clearResetRequirement() { this.mustResetPassword = false; save(); }

    public Long userId() { return userId; }

    private void ensureSessionSp(Context context) {
        if (sessionSP == null) {
            sessionSP = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }
}
