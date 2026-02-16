package com.bestbymanager.app.utilities;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import com.bestbymanager.app.UI.activities.EmployeeList;
import com.bestbymanager.app.UI.activities.UnlockKioskActivity;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.R;

public final class AdminMenu {
    private AdminMenu() {}

    /** Call from onCreateOptionsMenu */
    public static void inflateIfAdmin(Activity a, Menu menu) {
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

            a.startActivity(new Intent(a, EmployeeList.class)
                    .putExtra("selectMode", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            a.finish();
            return true;
        }

        if (id == R.id.action_lock_kiosk) {
            // admin-only hard lock
            if (!ActiveEmployeeManager.isActiveEmployeeAdmin(a)) return true;

            ActiveEmployeeManager.clearActiveEmployee(a);
            Session.get().lockKiosk(a);

            a.startActivity(new Intent(a, UnlockKioskActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            a.finish();
            return true;
        }
        return false;
    }
}