package com.bestbymanager.app.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.OffsetDateTime;

/** @noinspection ALL*/
@Entity(tableName = "employee", indices = @Index(value = "employeeName", unique = true))
public class Employee {
    @PrimaryKey(autoGenerate = true)
    private long employeeID;
    @NonNull
    private String employeeName;
    @NonNull
    private String hash;
    private byte[] thumbnail;
    public boolean isAdmin;
    public boolean isOwner;
    @Nullable public String employeePinHash;
    public int employeePinFailedAttempts;
    @Nullable public Long employeePinLockedUntil;
    @Nullable String resetTokenHash;
    @Nullable
    OffsetDateTime resetExpires;
    boolean   mustChange;

    // Empty constructor for UI layer – ignored by Room
    @Ignore
    public Employee() {}

    // Constructor for inserting first employee
    @Ignore
    public Employee(@NonNull String employeeName, @NonNull String hash) {
        this.employeeName = employeeName;
        this.hash = hash;
    }

    // Constructor for Room to read the database
    public Employee(long employeeID,
                    @NonNull String employeeName,
                    @NonNull String hash,
                    byte[] thumbnail,
                    boolean isAdmin,
                    boolean isOwner,
                    @Nullable String employeePinHash,
                    int employeePinFailedAttempts,
                    @Nullable Long employeePinLockedUntil,
                    @Nullable String resetTokenHash,
                    @Nullable OffsetDateTime resetExpires,
                    boolean mustChange) {
        this.employeeID = employeeID;
        this.employeeName = employeeName;
        this.hash = hash;
        this.thumbnail = thumbnail;
        this.isAdmin = isAdmin;
        this.isOwner = isOwner;
        this.employeePinHash = employeePinHash;
        this.employeePinFailedAttempts = employeePinFailedAttempts;
        this.employeePinLockedUntil = employeePinLockedUntil;
        this.resetTokenHash = resetTokenHash;
        this.resetExpires   = resetExpires;
        this.mustChange     = mustChange;
    }

    public long getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(long employeeID) { this.employeeID = employeeID; }
    @NonNull
    public String getHash() {
        return hash;
    }
    public void setHash(@NonNull String hash) {
        this.hash = hash;
    }
    @NonNull
    public String getEmployeeName() {
        return employeeName;
    }
    public void setEmployeeName(@NonNull String employeeName) {
        this.employeeName = employeeName;
    }
    public byte[] getThumbnail() { return thumbnail; }
    public void setThumbnail(byte[] thumbnail) { this.thumbnail = thumbnail; }

    public boolean isAdmin() { return isAdmin; }

    public void setAdmin(boolean admin) { isAdmin = admin; }
    public boolean isOwner() { return isOwner; }
    public void setOwner(boolean owner) { isOwner = owner; }
    @Nullable
    public String getEmployeePinHash() { return employeePinHash; }
    public void setEmployeePinHash(@Nullable String employeePinHash) { this.employeePinHash = employeePinHash; }
    public int getEmployeePinFailedAttempts() { return employeePinFailedAttempts; }
    public void setEmployeePinFailedAttempts(int attempts) { this.employeePinFailedAttempts = attempts; }
    @Nullable
    public Long getEmployeePinLockedUntil() { return employeePinLockedUntil; }
    public void setEmployeePinLockedUntil(@Nullable Long lockedUntil) { this.employeePinLockedUntil = lockedUntil; }
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
    public String toString() { return employeeName; }
}

