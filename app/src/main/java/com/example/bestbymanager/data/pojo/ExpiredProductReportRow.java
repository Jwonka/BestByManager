package com.example.bestbymanager.data.pojo;

import androidx.room.ColumnInfo;
import java.time.LocalDate;

public class ExpiredProductReportRow {
    @ColumnInfo(name = "prod_productID")
    public int       productID;

    @ColumnInfo(name = "prod_productName")
    public String    productName;

    @ColumnInfo(name = "prod_expirationDate")
    public LocalDate expirationDate;

    public int       expiredCount;
}
