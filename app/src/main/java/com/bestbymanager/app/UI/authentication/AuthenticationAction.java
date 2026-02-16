package com.bestbymanager.app.UI.authentication;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.bestbymanager.app.data.database.Repository;

public abstract class AuthenticationAction {

    protected final Context context;
    private final TextView nameField;
    private final EditText passwordField;
    protected final Repository repository;
    private final boolean strict;

    protected AuthenticationAction(Context context, TextView nameField, EditText passwordField, Repository repository, boolean strict) {
        this.context = context;
        this.nameField = nameField;
        this.passwordField = passwordField;
        this.repository = repository;
        this.strict = strict;
    }

    public final void run() {
        String name = nameField.getText().toString().trim();
        String plainPassword = passwordField.getText().toString();

        if (!validInput(name, plainPassword, strict)) return;

        performAuthorization(name, plainPassword);
    }

    protected boolean validInput(String name, String plainPassword, boolean strict) {

        if (name.isEmpty()) return fail(nameField, "Name required.");

        if (name.length() > 64) return fail(nameField, "Name must be less than 64 characters.");

        return !strict || strongEnough(plainPassword);
    }

    protected boolean strongEnough(String plainPassword) {

        if (plainPassword.isEmpty())
            return fail(passwordField, "Password required.");

        if (plainPassword.length() < 8)
            return fail(passwordField, "Password must be at least 8 characters.");

        if (plainPassword.length() > 128)
            return fail(passwordField, "Password must be less than 128 characters.");

        return true;
    }

    protected boolean fail(TextView field, String message) {
        field.setError(message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        return false;
    }

    protected abstract void performAuthorization(String name, String plainPassword);
}