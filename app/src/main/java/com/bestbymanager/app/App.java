package com.bestbymanager.app;

import android.app.Application;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.utilities.NotificationHelper;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Session.get().preload(this);
        NotificationHelper.ensureChannelExists(this);
    }
}
