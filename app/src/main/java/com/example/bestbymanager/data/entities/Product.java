package com.example.bestbymanager.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.time.LocalDate;
import java.util.Arrays;

@Entity(tableName = "product")
public class Product {
    @PrimaryKey(autoGenerate = true)
    private int productID;
    @NonNull
    private String productName;
    @NonNull
    private LocalDate expirationDate;
    private int    quantity;
    private String weight;
    private String barcode;
    private String brand;
    private String category;
    private LocalDate purchaseDate;
    private String imageUri;
    private byte[] thumbnail;


    // Constructor for adding a product **Room generates primary key**
    @Ignore
    public Product(@NonNull String productName, @NonNull LocalDate expirationDate) {
        this.productName = productName;
        this.expirationDate = expirationDate;
    }

    // Constructor used by Room to read the database
    public Product(int productID,
                   @NonNull String productName,
                   @NonNull LocalDate expirationDate,
                   int quantity,
                   String weight,
                   String barcode,
                   String brand,
                   String category,
                   LocalDate purchaseDate,
                   String imageUri,
                   byte[] thumbnail) {
        this.productID = productID;
        this.productName = productName;
        this.expirationDate = expirationDate;
        this.quantity        = quantity;
        this.weight          = weight;
        this.barcode         = barcode;
        this.brand           = brand;
        this.category        = category;
        this.purchaseDate    = purchaseDate;
        this.imageUri        = imageUri;
        this.thumbnail       = thumbnail;
    }

    public int getProductID() {
        return productID;
    }
    @NonNull
    public String getProductName() {
        return productName;
    }
    public void setProductName(@NonNull String productName) {
        this.productName = productName;
    }
    @NonNull
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(@NonNull LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
    public byte[] getThumbnail() { return thumbnail; }
    public void setThumbnail(byte[] thumbnail) { this.thumbnail = thumbnail; }

    @NonNull
    @Override
    public String toString() {
        return "Product{" +
                "productID=" + productID +
                ", productName='" + productName + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", quantity=" + quantity +
                ", weight='" + weight + '\'' +
                ", barcode='" + barcode + '\'' +
                ", brand='" + brand + '\'' +
                ", category='" + category + '\'' +
                ", purchaseDate='" + purchaseDate + '\'' +
                ", imageUri='" + imageUri + '\'' +
                ", thumbnail=" + Arrays.toString(thumbnail) +
                '}';
    }
}