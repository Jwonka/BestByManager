package com.bestbymanager.app.UI.adapter;

import androidx.annotation.Nullable;
import com.bestbymanager.app.data.entities.User;

public final class LoginResult {
    public enum Code { OK, EXPIRED, MUST_RESET, BAD_CREDENTIALS }
    public final Code   code;
    public final @Nullable User user;

    public static LoginResult ok(User user)            { return new LoginResult(Code.OK, user); }
    public static LoginResult badCredentials()      { return new LoginResult(Code.BAD_CREDENTIALS, null); }
    public static LoginResult mustReset(User user)     { return new LoginResult(Code.MUST_RESET, user); }
    public static LoginResult expired()         { return new LoginResult(Code.EXPIRED, null); }
    public LoginResult(Code code, @Nullable User user) {
        this.code = code;  this.user = user;
    }
}
