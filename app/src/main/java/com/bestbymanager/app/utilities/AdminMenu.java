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
        // Switch employee is always allowed when kiosk is unlocked
        MenuItem switchItem = menu.findItem(R.id.action_switch_employee);
        if (switchItem != null) switchItem.setVisible(Session.get().isUnlocked());

        // Lock kiosk is admin-only
        MenuItem lockItem = menu.findItem(R.id.action_lock_kiosk);
        if (lockItem != null) lockItem.setVisible(
                Session.get().isUnlocked() && ActiveEmployeeManager.isActiveEmployeeAdmin(a)
        );
    }

    /** Call from onOptionsItemSelected */
    public static boolean handle(Activity a, MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_switch_employee) {
            ActiveEmployeeManager.clearActiveEmployee(a);
            Router.routeAfterUnlock(a);  // will send to EmployeeList(selectMode=true) when >1
            return true;
        }

        if (id == R.id.action_lock_kiosk) {
            // admin-only hard lock
            if (!ActiveEmployeeManager.isActiveEmployeeAdmin(a)) return true;
            Session.get().lockKiosk(a); // clear active employee
            Router.routeAfterUnlock(a);
            return true;
        }
        return false;
    }
}