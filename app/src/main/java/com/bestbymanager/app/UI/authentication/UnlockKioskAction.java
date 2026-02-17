package com.bestbymanager.app.UI.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.bestbymanager.app.utilities.Router;
import com.bestbymanager.app.UI.activities.ResetPasswordActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;

public class UnlockKioskAction extends AuthenticationAction {

    public UnlockKioskAction(Context context, EditText name, EditText pass, Repository repository) {
        super(context, name, pass, repository, false);
    }

    @Override
    protected void performAuthorization(String name, String plainPassword) {
        repository.unlockKiosk(name, plainPassword)
                .observe((LifecycleOwner) context, res -> {
                    if (res == null) return;

                    switch (res.code) {
                        case OK: {
                            Employee e = res.employee;
                            if (e == null || !e.isAdmin()) {
                                Toast.makeText(context, "Only an administrator can unlock the kiosk.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Session.get().unlockKiosk(e, context);
                            ActiveEmployeeManager.clearActiveEmployee(context);

                            Router.routeAfterUnlock((Activity) context);
                            ((Activity) context).finish();
                            break;
                        }

                        case MUST_RESET: {
                            Employee e = res.employee;
                            if (e != null) {
                                Session.get().startLimited(e.getEmployeeID(), context);

                                context.startActivity(new Intent(context, ResetPasswordActivity.class)
                                        .putExtra("employeeId", e.getEmployeeID())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                ((Activity) context).finish();
                            }
                            break;
                        }

                        case BAD_CREDENTIALS:
                            Toast.makeText(context, "Invalid name or password.", Toast.LENGTH_SHORT).show();
                            break;

                        case EXPIRED:
                            Toast.makeText(context, "Temporary password expired. Ask an admin for a new one.", Toast.LENGTH_SHORT).show();
                            Session.get().lockKiosk(context);
                            break;

                        default:
                            Toast.makeText(context, "Unlock failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
