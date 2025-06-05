package com.example.bestbymanager.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "user", indices = @Index(value = "userName", unique = true))
public class User {
    @PrimaryKey(autoGenerate = true)
    private int userID;
    @NonNull
    private String userName;
    @NonNull
    private String hash;
    public boolean isAdmin;

    // Constructor for inserting new user
    @Ignore
    public User(@NonNull String userName, @NonNull String hash) {
        this.userName = userName;
        this.hash = hash;
    }

    // Constructor for Room to read the database
    public User(int userID, @NonNull String userName, @NonNull String hash) {
        this.userID = userID;
        this.userName = userName;
        this.hash = hash;
    }

    public int getUserID() {
        return userID;
    }

    @NonNull
    public String getHash() {
        return hash;
    }
    public void setHash(@NonNull String hash) {
        this.hash = hash;
    }
    @NonNull
    public String getUserName() {
        return userName;
    }
    public void setUserName(@NonNull String userName) {
        this.userName = userName;
    }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "userID=" + userID +
                ", userName='" + userName + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}

