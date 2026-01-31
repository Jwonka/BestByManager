package com.bestbymanager.app.data.entities;

import static androidx.room.ForeignKey.CASCADE;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.LocalDate;
import java.util.Arrays;

/** @noinspection NotNullFieldNotInitialized, unused */
@Entity(tableName = "product",
        indices = {
                @Index(value = "userID"),
                @Index(value = {"expirationDate", "brand"}),
                @Index(value = {"barcode", "expirationDate"}),
                @Index(value = {"userID", "expirationDate"}),
                @Index(value = {"productName", "expirationDate"})
        },
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userID",
                childColumns = "userID",
                onDelete = CASCADE))
public class Product {
    @PrimaryKey(autoGenerate = true)
    private long productID;
    @ColumnInfo(defaultValue = "1")
    public long userID;
    private String brand;
    @NonNull
    private String productName;
    @NonNull
    private LocalDate expirationDate;
    private int    quantity;
    private String weight;
    private String barcode;
    private int category;
    private int isle;
    private LocalDate purchaseDate;
    private String imageUri;
    private byte[] thumbnail;

    @Ignore
    public Product() {}

    // Constructor for adding a product **Room generates primary key**
    @Ignore
    public Product(@NonNull String productName, @NonNull LocalDate expirationDate) {
        this.productName = productName;
        this.expirationDate = expirationDate;
    }

    // Constructor used by Room to read the database
    public Product(long productID,
                   long userID,
                   @NonNull String productName,
                   @NonNull LocalDate expirationDate,
                   int quantity,
                   String weight,
                   String barcode,
                   String brand,
                   int category,
                   int isle,
                   LocalDate purchaseDate,
                   String imageUri,
                   byte[] thumbnail) {
        this.productID = productID;
        this.userID        = userID;
        this.brand           = brand;
        this.productName = productName;
        this.expirationDate = expirationDate;
        this.quantity        = quantity;
        this.weight          = weight;
        this.barcode         = barcode;
        this.category        = category;
        this.isle            = isle;
        this.purchaseDate    = purchaseDate;
        this.imageUri        = imageUri;
        this.thumbnail       = thumbnail;
    }

    public long getProductID() {
        return productID;
    }
    public void setProductID(long productID) { this.productID = productID; }

    public long getUserID()   { return userID; }
    public void setUserID(long id) { this.userID = id; }
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
    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }
    public int getIsle() { return isle; }
    public void setIsle(int isle) { this.isle = isle; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
    public byte[] getThumbnail() { return thumbnail; }
    public void setThumbnail(byte[] thumbnail) { this.thumbnail = thumbnail; }
    public boolean isExpired() { return expirationDate != null &&  expirationDate.isBefore(LocalDate.now()); }
    public boolean isDiscardable() { return expirationDate != null && !expirationDate.isAfter(LocalDate.now()); }

    @NonNull
    @Override
    public String toString() {
        return "Product{" +
                "productID=" + productID +
                ", userID="+ userID +
                ", brand='" + brand + '\'' +
                ", productName='" + productName + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", quantity=" + quantity +
                ", weight='" + weight + '\'' +
                ", barcode='" + barcode + '\'' +
                ", category='" + category + '\'' +
                ", isle='" + isle + '\'' +
                ", purchaseDate='" + purchaseDate + '\'' +
                ", imageUri='" + imageUri + '\'' +
                ", thumbnail=" + Arrays.toString(thumbnail) +
                '}';
    }
}