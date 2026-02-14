package com.bestbymanager.app.utilities;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import com.bestbymanager.app.UI.activities.UserList;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.R;

public final class AdminMenu {
    private AdminMenu() {}

    /** Call from onCreateOptionsMenu */
    public static void inflateIfAdmin(Activity a, Menu menu) {
        if (Session.get().currentUserIsAdmin()) {
            a.getMenuInflater().inflate(R.menu.menu_admin_logout, menu);
        }
    }

    /** Call from onPrepareOptionsMenu for dynamic visibility */
    public static void setVisibility(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_admin_logout);
        if (item != null) item.setVisible(Session.get().currentUserIsAdmin());
    }

    /** Call from onOptionsItemSelected */
    public static boolean handle(Activity a, MenuItem item) {
        if (item.getItemId() != R.id.action_admin_logout) return false;

        Session.get().logOut(a);
        ActiveEmployeeManager.clearActiveEmployee(a);

        Intent i = new Intent(a, UserList.class)
                .putExtra("selectMode", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
        return true;
    }
}
