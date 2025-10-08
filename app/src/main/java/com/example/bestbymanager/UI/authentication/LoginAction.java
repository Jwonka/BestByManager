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
import com.example.bestbymanager.data.entities.User;

public class LoginAction extends AuthenticationAction {
    public LoginAction(Context context, EditText user, EditText pass, Repository repository) {
        super(context, user, pass, repository, false);
    }

    @Override
    protected void performAuthorization(String name, String plainPassword) {
        repository.login(name, plainPassword).observe((LifecycleOwner) context, loginResult -> {
            if(loginResult == null) { return; }
            switch (loginResult.code) {
                case OK:
                    User user = loginResult.user;
                    if (user != null) {
                        if (Session.get().isLoggedOut()) {
                            Toast.makeText(context, "Session not set!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        enterApp(user);
                        break;
                    }

                    case MUST_RESET: {
                        // valid temp → limited session so Reset screen knows the user
                        if (loginResult.user != null) {
                            Session.get().startLimited(loginResult.user.getUserID());

                            Intent i = new Intent(context, ResetPasswordActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            i.putExtra("userId", loginResult.user.getUserID());
                            context.startActivity(i);
                        }
                        break;
                    }

                case EXPIRED:
                    Toast.makeText(context, "Temporary password has expired.  Ask an admin for a new one.", Toast.LENGTH_SHORT).show();
                    Session.get().logOut(context);
                    break;

                case BAD_CREDENTIALS:
                default:
                    Toast.makeText(context, "Invalid username or password.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enterApp(User user) {
        Intent deep = ((Activity) context).getIntent().getParcelableExtra("deepLink");
        Intent target = deep != null ? deep : new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(target);
        String welcome = user.isAdmin ? "Welcome administrator " + user.getUserName() : "Welcome " + user.getUserName();
        Toast.makeText(context, welcome, Toast.LENGTH_SHORT).show();
        ((Activity) context).finish();
    }
}
