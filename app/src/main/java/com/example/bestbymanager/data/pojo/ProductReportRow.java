package com.example.bestbymanager.data.pojo;

import java.time.LocalDate;

public class ProductReportRow {
    public long productID;

    public String brand;
    public String productName;
    public LocalDate expirationDate;
    public LocalDate purchaseDate;

    public int quantity;
    public String enteredBy;
    public String    barcode;
    public String    category;
}
