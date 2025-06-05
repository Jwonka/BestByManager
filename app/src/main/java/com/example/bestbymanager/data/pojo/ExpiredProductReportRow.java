package com.example.bestbymanager.data.pojo;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import com.example.bestbymanager.data.entities.Product;

public class ExpiredProductReportRow {
    @Embedded(prefix = "prod_")
    public Product product;

    @ColumnInfo(name = "expiredCount") public int expiredCount;
}
