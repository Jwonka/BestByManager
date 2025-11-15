package com.example.bestbymanager.data.pojo;

import androidx.room.RoomWarnings;
import java.time.LocalDate;
@SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
public class ProductReportRow {
    public long productID;

    public String brand;
    public String productName;
    public LocalDate expirationDate;
    public LocalDate purchaseDate;
    public int quantity;
    public String enteredBy;
    public String    barcode;
    /** @noinspection unused*/
    public String    category;
}
