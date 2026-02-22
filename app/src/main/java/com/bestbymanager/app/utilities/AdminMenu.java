package com.bestbymanager.app.utilities;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.R;

public final class AdminMenu {
    private AdminMenu() {}

    /** Call from onCreateOptionsMenu */
    public static void inflateKioskActions(Activity a, Menu menu) {
        // Always inflate the menu that contains BOTH actions.
        a.getMenuInflater().inflate(R.menu.menu_kiosk_actions, menu);
    }

    /** Call from onPrepareOptionsMenu for dynamic visibility */
    public static void setVisibility(Activity a, Menu menu) {
        boolean unlocked = Session.get().isUnlocked();
        boolean hasActive = ActiveEmployeeManager.hasActiveEmployee(a);
        boolean activeIsAdmin = ActiveEmployeeManager.isActiveEmployeeAdmin(a);

        // Switch employee only when unlocked AND an active employee exists
        MenuItem switchItem = menu.findItem(R.id.action_switch_employee);
        if (switchItem != null) switchItem.setVisible(unlocked && hasActive);

        // Lock kiosk only when unlocked AND active employee is admin
        MenuItem lockItem = menu.findItem(R.id.action_lock_kiosk);
        if (lockItem != null) lockItem.setVisible(unlocked && activeIsAdmin);
    }

    /** Call from onOptionsItemSelected */
    public static boolean handle(Activity a, MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_switch_employee) {
            if (!Session.get().isUnlocked()) return true;
            // If no active employee yet, ignore
            if (!ActiveEmployeeManager.hasActiveEmployee(a)) return true;

            ActiveEmployeeManager.clearActiveEmployee(a);
            Router.routeAfterUnlock(a);  // will send to EmployeeList(selectMode=true) when > 1
            return true;
        }

        if (id == R.id.action_lock_kiosk) {
            if (!Session.get().isUnlocked()) return true;
            // admin-only hard lock
            if (!ActiveEmployeeManager.isActiveEmployeeAdmin(a)) return true;
            Session.get().lockKiosk(a); // clear active employee
            Router.routeAfterUnlock(a);
            return true;
        }
        return false;
    }
}