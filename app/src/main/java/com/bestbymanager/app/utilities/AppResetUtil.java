package com.bestbymanager.app.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;
import com.bestbymanager.app.UI.activities.LoginActivity;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.data.database.ProductDatabaseBuilder;
import java.io.File;

public final class AppResetUtil {
    private AppResetUtil() {}

    public static void wipeAllAndRestart(Activity a) {
        Context app = a.getApplicationContext();

        try {
            Session.get().logOut(app);
            try {
                ProductDatabaseBuilder db = ProductDatabaseBuilder.getDatabase(app);
                if (db != null) db.close();
            } catch (Throwable ignored) {}

            app.deleteDatabase(ProductDatabaseBuilder.DB_NAME);

            app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply();
            app.getSharedPreferences("bestby_session", Context.MODE_PRIVATE).edit().clear().apply();
            app.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit().clear().apply();
            PreferenceManager.getDefaultSharedPreferences(app).edit().clear().apply();
            deleteRecursively(app.getCacheDir());

            Intent i = new Intent(a, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            a.startActivity(i);
            a.finishAffinity();

        } catch (Throwable ignored) {}
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}

