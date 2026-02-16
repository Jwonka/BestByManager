package com.bestbymanager.app.data.pojo;

import androidx.annotation.Nullable;
import androidx.room.Ignore;

public class EmployeeReportRow {
    public long employeeID;

    public String employeeName;

    public String brand;

    public String productName;
    public String lastDiscardNote;
    @Nullable public Integer expiredCount;
    @Nullable public Integer goodCount;
    @Nullable public Integer totalCount;
    @Nullable public Integer discardedCount;
    @Nullable public Integer lotCount;
    @Ignore public boolean isFooter;
    @Ignore public boolean isHeader;
}