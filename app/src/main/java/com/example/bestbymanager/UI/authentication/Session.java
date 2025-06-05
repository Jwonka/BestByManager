package com.example.bestbymanager.UI.authentication;

import com.example.bestbymanager.data.entities.User;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Session {
    private static final Session INSTANCE = new Session();
    private final AtomicInteger uid = new AtomicInteger(-1);
    private final AtomicReference<String> userName = new AtomicReference<>("");
    private final AtomicBoolean admin = new AtomicBoolean(false);

    private Session() {}
    public static Session get() { return INSTANCE; }
    public void logIn(User user) {
        uid.set(user.getUserID());
        userName.set(user.getUserName());
        admin.set(user.isAdmin);
    }
    public int currentUserId() { return uid.get(); }
    public String currentUserName() { return userName.get(); }
    public boolean currentUserIsAdmin() { return admin.get(); }
}
