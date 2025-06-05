package com.example.bestbymanager.UI.authentication;

import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;
import com.example.bestbymanager.data.database.Repository;

public abstract class AuthenticationAction {

    protected final Context context;
    private final EditText userName;
    private final EditText password;
    protected final Repository repository;
    private final boolean strict;
    protected AuthenticationAction(Context context, EditText userName, EditText password, Repository repository, boolean strict) {
        this.context = context;
        this.userName = userName;
        this.password = password;
        this.repository = repository;
        this.strict = strict;
    }
    public final void run() {
        String name = userName.getText().toString().trim();
        String plainPassword = password.getText().toString().trim();

        if (!validInput(name, plainPassword, strict)) return;

        performAuthorization(name, plainPassword);
    }

    private boolean validInput(String name, String plainPassword, boolean strict) {

        if (name.isEmpty()) { return fail(userName, "Username required."); }

        if (name.length() > 30) { return fail(userName, "Username must be less than 30 characters."); }

        if(plainPassword.isEmpty()) { return fail(password, "password required."); }

        if (plainPassword.length() > 30) { return fail(password, "Password must be less than 30 characters."); }

        return !strict || strongEnough(plainPassword);
    }

    private boolean strongEnough(String plainPassword) {
        if(plainPassword.length() < 12) return fail(password,"Password must be 12-characters minimum.");

        if(plainPassword.chars().noneMatch(Character::isDigit)) return fail(password,"Password must contain a number.");

        if(plainPassword.chars().allMatch(Character::isLetterOrDigit)) return fail(password,"Password must contain a symbol.");

        if(plainPassword.chars().noneMatch(Character::isUpperCase)) return fail(password,"Password must contain a uppercase letter.");

        if(plainPassword.chars().noneMatch(Character::isLowerCase)) return fail(password,"Password must contain a lowercase letter.");

        return true;
    }

    private boolean fail(EditText field, String message) {
        field.setError(message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        return false;
    }

    protected abstract void performAuthorization(String name, String plainPassword);
}
