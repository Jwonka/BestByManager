package com.example.bestbymanager.UI.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.example.bestbymanager.UI.activities.MainActivity;
import com.example.bestbymanager.UI.activities.ResetPasswordActivity;
import com.example.bestbymanager.data.database.Repository;

public class LoginAction extends AuthenticationAction {
    public LoginAction(Context context, EditText user, EditText pass, Repository repository) {
        super(context, user, pass, repository, false);
    }

    @Override
    protected void performAuthorization(String name, String plainPassword) {
        repository.login(name, plainPassword).observe((LifecycleOwner) context, user -> {
            if (user != null) {
                if (Session.get().isLoggedOut()) {
                    Toast.makeText(context, "Session not set!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (user.isMustChange()) {
                    Intent reset = new Intent(context, ResetPasswordActivity.class).putExtra("userID", user.getUserID());
                    context.startActivity(reset);
                    ((Activity) context).finish();
                    return;
                }

                Intent deep = ((Activity) context).getIntent().getParcelableExtra("deepLink");
                Intent target = deep != null ? deep : new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(target);
                String welcome = user.isAdmin ? "Welcome administrator " + user.getUserName() : "Welcome " + user.getUserName();
                Toast.makeText(context, welcome, Toast.LENGTH_SHORT).show();
                ((Activity) context).finish();
            } else {
                Toast.makeText(context, "Invalid username or password.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
