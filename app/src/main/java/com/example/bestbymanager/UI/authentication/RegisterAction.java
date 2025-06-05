package com.example.bestbymanager.UI.authentication;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import com.example.bestbymanager.UI.activities.MainActivity;
import com.example.bestbymanager.data.database.Repository;

public class RegisterAction extends AuthenticationAction {
    public RegisterAction(Context context, EditText user, EditText pass, Repository repository) {
        super(context, user, pass, repository, true);
    }
    @Override
    protected void performAuthorization(String name, String plainPassword) {
        repository.insertUser(name, plainPassword).observe((LifecycleOwner) context, user -> {
            if(user != null) {
                context.startActivity(new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                if (user.isAdmin) {
                    Toast.makeText(context, "Administration account created. Welcome " + user.getUserName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Account created. Welcome " + user.getUserName(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Username already taken.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
