package com.bestbymanager.app.UI.authentication;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.bestbymanager.app.UI.activities.MainActivity;
import com.bestbymanager.app.UI.activities.UnlockKioskActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.session.Session;

public class ResetPasswordAction extends AuthenticationAction {
    private final EditText confirm;
    private final long     employeeID;
    private Employee employee;

    public void setEmployee(Employee e) { this.employee = e; }

    public ResetPasswordAction(Context ctx, TextView employeeName, EditText pwd, EditText confirm, long employeeID, Repository repository) {
        super(ctx, employeeName, pwd, repository, true);
        this.confirm = confirm;
        this.employeeID  = employeeID;
    }

    @Override
    protected boolean validInput(String name, String pwd, boolean strict) {
        if (!strongEnough(pwd)) { return false; }

        String confirmPwd = confirm.getText().toString().trim();
        if (!pwd.equals(confirmPwd)) { return fail(confirm, "Passwords do not match."); }
        return true;
    }

    @Override
    protected void performAuthorization(String name, String plainPwd) {
        repository.changePassword(employeeID, plainPwd)
            .observe((LifecycleOwner) context, success -> {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(context, "Password updated.", Toast.LENGTH_SHORT).show();
                    Session.get().clearResetRequirement();
                    if (employee != null && employee.isAdmin()) {
                        // Admin reset: establish a full unlocked session so the kiosk gate passes.
                        Session.get().unlockKiosk(employee, context);
                        context.startActivity(new Intent(context, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    } else {
                        // Non-admin reset: cannot unlock the kiosk themselves and is sent to UnlockKioskActivity so an admin can unlock.
                        context.startActivity(new Intent(context, UnlockKioskActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }
                } else {
                    Toast.makeText(context, "Could not change password.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}