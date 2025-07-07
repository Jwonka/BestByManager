package com.example.bestbymanager.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Arrays;

@Entity(tableName = "user", indices = @Index(value = "userName", unique = true))
public class User {
    @PrimaryKey(autoGenerate = true)
    private long userID;
    @NonNull
    private String userName;
    @NonNull
    private String firstName;
    @NonNull
    private String lastName;
    @NonNull
    private String hash;
    private byte[] thumbnail;
    public boolean isAdmin;

    // Empty constructor for UI layer – ignored by Room
    @Ignore
    public User() {}

    // Constructor for inserting first user
    @Ignore
    public User(@NonNull String userName, @NonNull String hash) {
        this.userName = userName;
        this.hash = hash;
    }

    // Constructor for Room to read the database
    public User(long userID, @NonNull String userName, @NonNull String hash, @NonNull String firstName, @NonNull String lastName, byte[] thumbnail, boolean isAdmin) {
        this.userID = userID;
        this.userName = userName;
        this.hash = hash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.thumbnail = thumbnail;
        this.isAdmin = isAdmin;
    }

    public long getUserID() {
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
    public byte[] getThumbnail() { return thumbnail; }
    public void setThumbnail(byte[] thumbnail) { this.thumbnail = thumbnail; }
    @NonNull
    public String getLastName() { return lastName; }
    public void setLastName(@NonNull String lastName) { this.lastName = lastName; }
    @NonNull
    public String getFirstName() { return firstName; }
    public void setFirstName(@NonNull String firstName) { this.firstName = firstName; }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    @Override
    public String toString() { return firstName + " " + lastName + " (" + userName + ")"; }
}

