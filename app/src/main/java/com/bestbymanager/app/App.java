package com.bestbymanager.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.bestbymanager.app.session.DeviceOwnerManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.utilities.NotificationHelper;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        int themeMode = DeviceOwnerManager.getThemeMode(this);
        switch (themeMode) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        Session.get().preload(this);
        NotificationHelper.ensureChannelExists(this);
    }
}
