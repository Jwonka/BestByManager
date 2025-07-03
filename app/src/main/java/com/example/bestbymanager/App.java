package com.example.bestbymanager;

import android.app.Application;
import com.example.bestbymanager.utilities.NotificationHelper;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannelExists(this);
    }
}
