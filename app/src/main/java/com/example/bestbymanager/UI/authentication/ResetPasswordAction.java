package com.example.bestbymanager.UI.authentication;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.example.bestbymanager.UI.activities.MainActivity;
import com.example.bestbymanager.data.database.Repository;

public class ResetPasswordAction extends AuthenticationAction {
    private final EditText confirm;
    private final long     userID;

    public ResetPasswordAction(Context ctx, TextView username, EditText pwd, EditText confirm, long userID, Repository repository) {
        super(ctx, username, pwd, repository, true);
        this.confirm = confirm;
        this.userID  = userID;
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
        repository.changePassword(userID, plainPwd)
            .observe((LifecycleOwner) context, success -> {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(context, "Password updated.", Toast.LENGTH_SHORT).show();
                    context.startActivity(new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                } else {
                    Toast.makeText(context, "Could not change password.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}