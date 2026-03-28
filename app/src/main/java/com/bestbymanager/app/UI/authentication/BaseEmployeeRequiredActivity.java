package com.bestbymanager.app.UI.authentication;

import android.content.Intent;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import com.bestbymanager.app.UI.activities.MainActivity;
import com.bestbymanager.app.UI.activities.UnlockKioskActivity;
import com.bestbymanager.app.UI.activities.EmployeeList;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.DeviceOwnerManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.utilities.IdleManager;
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
            int count = 0;
            try { count = repo.employeeCountBlocking(); } catch (Throwable ignored) {}

            boolean unlocked = Session.get().isUnlocked() && !Session.get().requiresPasswordReset();
            boolean hasActive = ActiveEmployeeManager.hasActiveEmployee(this);

            // validate active employee still exists (deleted active employee case)
            boolean activeExists = true;
            if (hasActive) {
                try {
                    long activeId = ActiveEmployeeManager.getActiveEmployeeId(this);
                    activeExists = repo.employeeExistsBlocking(activeId);
                } catch (Throwable ignored) {
                    activeExists = true; // fail open for DB errors
                }
            }

            long onlyId = -1L;
            boolean onlyIsAdmin = false;
            boolean shouldAutoSelect = unlocked && !hasActive && count == 1;

            if (shouldAutoSelect) {
                try {
                    onlyId = repo.getOnlyEmployeeIdBlocking();
                    onlyIsAdmin = repo.isEmployeeAdminBlocking(onlyId);
                } catch (Throwable ignored) {}
            }

            final int fCount = count;
            final boolean fUnlocked = unlocked;
            final boolean fHasActive = hasActive;
            final boolean fActiveExists = activeExists;
            final long fOnlyId = onlyId;
            final boolean fOnlyIsAdmin = onlyIsAdmin;
            final boolean fAutoSelect = shouldAutoSelect && onlyId > 0;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (ActiveEmployeeManager.hasActiveEmployee(this) &&
                        IdleManager.isExpired(this)) {

                    boolean lock = DeviceOwnerManager.isLockAfterIdleEnabled(this);

                    IdleManager.clear(this);

                    if (lock) {
                        Session.get().lockKiosk(this);

                        startActivity(new Intent(this, UnlockKioskActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    } else {
                        ActiveEmployeeManager.clearActiveEmployee(this);

                        startActivity(new Intent(this, EmployeeList.class)
                                .putExtra("selectMode", true)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }

                    finish();
                    return;
                }

                // 1) No employees → first-time setup (unlock/admin creation)
                if (fCount == 0) {
                    startActivity(new Intent(this, UnlockKioskActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                // 2) Kiosk locked → require admin unlock OR reset required
                if (!fUnlocked) {
                    startActivity(new Intent(this, UnlockKioskActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                // active employee was deleted -> force selection
                if (fHasActive && !fActiveExists) {
                    ActiveEmployeeManager.clearActiveEmployee(this);
                    startActivity(new Intent(this, EmployeeList.class)
                            .putExtra("selectMode", true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                // 3) Kiosk unlocked but no active employee selected
                if (!fHasActive) {
                    // auto-select when exactly 1 employee exists
                    if (fAutoSelect) {
                        ActiveEmployeeManager.setActiveEmployeeId(this, fOnlyId);
                        ActiveEmployeeManager.setActiveEmployeeIsAdmin(this, fOnlyIsAdmin);

                        // selection just occurred -> collapse to Main
                        startActivity(new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                        return;
                    }

                    startActivity(new Intent(this, EmployeeList.class)
                            .putExtra("selectMode", true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                if (gatePassed) return;
                gatePassed = true;
                onGatePassed();
                ViewCompat.requestApplyInsets(getWindow().getDecorView());
            });
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        IdleManager.touch(this);
        return super.dispatchTouchEvent(ev);
    }

    /** Called exactly once per Activity instance, after kiosk+employee gates pass. */
    protected abstract void onGatePassed();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}