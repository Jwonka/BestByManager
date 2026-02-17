package com.bestbymanager.app.UI.authentication;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.bestbymanager.app.UI.activities.UnlockKioskActivity;
import com.bestbymanager.app.UI.activities.EmployeeList;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseEmployeeRequiredActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile boolean gatePassed = false;
    @Override
    protected void onResume() {
        super.onResume();

        Session.get().preload(this);

        io.execute(() -> {
            Repository repo = new Repository(getApplication());
            int count = repo.employeeCountBlocking();
            boolean unlocked = Session.get().isUnlocked();
            boolean hasActive = ActiveEmployeeManager.hasActiveEmployee(this);

            long onlyId = -1L;
            boolean onlyIsAdmin = false;
            boolean shouldAutoSelect = unlocked && !hasActive && count == 1;

            if (shouldAutoSelect) {
                onlyId = repo.getOnlyEmployeeIdBlocking();
                onlyIsAdmin = repo.isEmployeeAdminBlocking(onlyId);
            }

            final long fOnlyId = onlyId;
            final boolean fOnlyIsAdmin = onlyIsAdmin;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                // 1) No employees → first-time setup (unlock/admin creation)
                if (count == 0) {
                    startActivity(new Intent(this, UnlockKioskActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                // 2) Kiosk locked → require admin unlock
                if (!unlocked) {
                    startActivity(new Intent(this, UnlockKioskActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                // 3) Kiosk unlocked but no active employee selected
                if (!ActiveEmployeeManager.hasActiveEmployee(this)) {
                    // auto-select when exactly 1 employee exists
                    if (count == 1 && fOnlyId > 0) {
                        ActiveEmployeeManager.setActiveEmployeeId(this, fOnlyId);
                        ActiveEmployeeManager.setActiveEmployeeIsAdmin(this, fOnlyIsAdmin);
                    } else {

                        startActivity(new Intent(this, EmployeeList.class)
                                .putExtra("selectMode", true)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                        return;
                    }
                }

                if (gatePassed) return;
                gatePassed = true;
                onGatePassed();
            });
        });
    }

    /** Called exactly once per Activity instance, after kiosk+employee gates pass. */
    protected abstract void onGatePassed();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}