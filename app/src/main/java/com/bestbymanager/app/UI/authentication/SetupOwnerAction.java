package com.bestbymanager.app.UI.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.bestbymanager.app.UI.activities.MainActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.DeviceOwnerManager;
import com.bestbymanager.app.session.Session;

public class SetupOwnerAction extends AuthenticationAction {

    public SetupOwnerAction(Context context, EditText name, EditText pass, Repository repository) {
        super(context, name, pass, repository, true);
    }

    @Override
    protected void performAuthorization(String name, String plainPassword) {
        repository.insertEmployee(name, plainPassword)
                .observe((LifecycleOwner) context, employee -> {
                    if (employee == null) {
                        Toast.makeText(context, "Name already taken.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // First employee is admin in Repository.insertEmployee(...)
                    Session.get().unlockKiosk(employee, context);
                    DeviceOwnerManager.setOwnerEmployeeId(context, employee.getEmployeeID());
                    ActiveEmployeeManager.setActiveEmployeeId(context, employee.getEmployeeID());
                    ActiveEmployeeManager.setActiveEmployeeIsAdmin(context, employee.isAdmin());

                    Toast.makeText(context, "Owner created. Kiosk unlocked.", Toast.LENGTH_SHORT).show();

                    context.startActivity(new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    ((Activity) context).finish();
                });
    }
}
