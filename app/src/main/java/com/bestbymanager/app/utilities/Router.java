package com.bestbymanager.app.utilities;

import android.app.Activity;
import android.content.Intent;
import com.bestbymanager.app.UI.activities.EmployeeList;
import com.bestbymanager.app.UI.activities.MainActivity;
import com.bestbymanager.app.UI.activities.UnlockKioskActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Router {
    private Router() {}
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public static void routeAfterUnlock(Activity a) {
        Session.get().preload(a);

        IO.execute(() -> {
            Repository repo = new Repository(a.getApplication());
            int count = 0;
            try { count = repo.employeeCountBlocking(); } catch (Throwable ignored) {}

            // if exactly 1 employee and unlocked, auto-select
            long onlyId = -1L;
            boolean onlyIsAdmin = false;
            boolean unlocked = Session.get().isUnlocked() && !Session.get().requiresPasswordReset();

            if (unlocked && count == 1) {
                try {
                    onlyId = repo.getOnlyEmployeeIdBlocking();
                    onlyIsAdmin = repo.isEmployeeAdminBlocking(onlyId);
                } catch (Throwable ignored) {}
            }

            final int fCount = count;
            final boolean fUnlocked = unlocked;
            final long fOnlyId = onlyId;
            final boolean fOnlyIsAdmin = onlyIsAdmin;

            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;

                // No employees -> setup/unlock screen
                if (fCount == 0) {
                    a.startActivity(new Intent(a, UnlockKioskActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    a.finish();
                    return;
                }

                // Locked (or forced reset) -> unlock screen
                if (!fUnlocked) {
                    a.startActivity(new Intent(a, UnlockKioskActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    a.finish();
                    return;
                }

                // Unlocked but no active employee -> selection (or auto-select if only 1)
                if (!ActiveEmployeeManager.hasActiveEmployee(a)) {
                    if (fCount == 1 && fOnlyId > 0) {
                        ActiveEmployeeManager.setActiveEmployeeId(a, fOnlyId);
                        ActiveEmployeeManager.setActiveEmployeeIsAdmin(a, fOnlyIsAdmin);

                        a.startActivity(new Intent(a, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        a.finish();
                        return;
                    }

                    a.startActivity(new Intent(a, EmployeeList.class)
                            .putExtra("selectMode", true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    a.finish();
                    return;
                }

                // Unlocked + active employee -> main
                a.startActivity(new Intent(a, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                a.finish();
            });
        });
    }
}
