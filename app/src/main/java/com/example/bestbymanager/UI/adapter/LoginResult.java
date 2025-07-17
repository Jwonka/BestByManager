package com.example.bestbymanager.UI.adapter;

import androidx.annotation.Nullable;
import com.example.bestbymanager.data.entities.User;

public final class LoginResult {
    public enum Code { OK, EXPIRED, BAD_CREDENTIALS }
    public final Code   code;
    public final @Nullable User user;
    public LoginResult(Code code, @Nullable User user) {
        this.code = code;  this.user = user;
    }
}
