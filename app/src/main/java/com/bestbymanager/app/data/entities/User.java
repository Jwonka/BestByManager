package com.bestbymanager.app.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.OffsetDateTime;

/** @noinspection ALL*/
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
    @Nullable String resetTokenHash;
    @Nullable
    OffsetDateTime resetExpires;
    boolean   mustChange;

    // Empty constructor for UI layer – ignored by Room
    @Ignore
    public User() {}

    // Constructor for inserting first user
    @Ignore
    public User(@NonNull String userName, @NonNull String hash) {
        this.userName = userName;
        this.hash = hash;
        this.userName = userName;
        this.hash = hash;
    }

    // Constructor for Room to read the database
    public User(long userID,
                @NonNull String userName,
                @NonNull String hash,
                @NonNull String firstName,
                @NonNull String lastName,
                byte[] thumbnail,
                boolean isAdmin,
                @Nullable String resetTokenHash,
                @Nullable OffsetDateTime resetExpires,
                boolean mustChange) {
        this.userID = userID;
        this.userName = userName;
        this.hash = hash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.thumbnail = thumbnail;
        this.isAdmin = isAdmin;
        this.resetTokenHash = resetTokenHash;
        this.resetExpires   = resetExpires;
        this.mustChange     = mustChange;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) { this.userID = userID; }
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
    public void setResetTokenHash(@Nullable String token) { this.resetTokenHash = token; }
    @Nullable
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetExpires(@Nullable OffsetDateTime ts) { this.resetExpires = ts; }
    @Nullable
    public OffsetDateTime getResetExpires() { return resetExpires; }
    public void setMustChange(boolean flag) { this.mustChange = flag; }
    public boolean isMustChange() { return mustChange; }

    @NonNull
    @Override
    public String toString() { return firstName + " " + lastName + " (" + userName + ")"; }
}

