package com.bestbymanager.app.UI.adapter;

import androidx.annotation.Nullable;
import com.bestbymanager.app.data.entities.Employee;

public final class UnlockKioskResult {
    public enum Code { OK, EXPIRED, MUST_RESET, BAD_CREDENTIALS }
    public final Code   code;
    public final @Nullable Employee employee;

    public static UnlockKioskResult ok(Employee employee)            { return new UnlockKioskResult(Code.OK, employee); }
    public static UnlockKioskResult badCredentials()      { return new UnlockKioskResult(Code.BAD_CREDENTIALS, null); }
    public static UnlockKioskResult mustReset(Employee employee)     { return new UnlockKioskResult(Code.MUST_RESET, employee); }
    public static UnlockKioskResult expired()         { return new UnlockKioskResult(Code.EXPIRED, null); }
    public UnlockKioskResult(Code code, @Nullable Employee employee) { this.code = code;  this.employee = employee; }
}
