package com.bestbymanager.app.UI.authentication;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.bestbymanager.app.UI.activities.MainActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.Session;

public class ResetPasswordAction extends AuthenticationAction {
    private final EditText confirm;
    private final long     employeeID;

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
                    context.startActivity(new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                } else {
                    Toast.makeText(context, "Could not change password.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}