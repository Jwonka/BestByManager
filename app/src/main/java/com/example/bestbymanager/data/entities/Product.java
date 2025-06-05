package com.example.bestbymanager.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "product")
public class Product {
    @PrimaryKey(autoGenerate = true)
    private int productID;
    private String productName;
    private String expirationDate;


    // Constructor for adding a product **Room generates primary key**
    @Ignore
    public Product(String productName, String expirationDate) {
        this.productName = productName;
        this.expirationDate = expirationDate;
    }

    // Constructor used by Room to read the database
    public Product(int productID, String productName, String expirationDate) {
        this.productID = productID;
        this.productName = productName;
        this.expirationDate = expirationDate;
    }

    public int getProductID() {
        return productID;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }


    @NonNull
    @Override
    public String toString() {
        return "Product{" +
                "productName='" + productName + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                '}';
    }
}