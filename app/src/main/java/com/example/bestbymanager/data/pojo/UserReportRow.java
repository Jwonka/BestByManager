package com.example.bestbymanager.data.pojo;

import androidx.annotation.Nullable;
import androidx.room.Ignore;

public class UserReportRow {
    public long userID;

    public String userName;

    public String firstName;

    public String lastName;

    public String brand;

    public String productName;
    @Nullable
    public Integer expiredCount;
    @Nullable
    public Integer goodCount;
    @Nullable
    public Integer totalCount;
    @Ignore public boolean isFooter;
    @Ignore public boolean isHeader;
}
