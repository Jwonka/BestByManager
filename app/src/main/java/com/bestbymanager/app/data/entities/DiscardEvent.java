package com.bestbymanager.app.data.entities;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "discard_event",
        indices = {
                @Index(value = {"productID"}),
                @Index(value = {"userID"}),
                @Index(value = {"createdAt"})
        },
        foreignKeys = {
                @ForeignKey(
                        entity = Product.class,
                        parentColumns = "productID",
                        childColumns = "productID",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "userID",
                        childColumns = "userID",
                        onDelete = CASCADE
                )
        }
)
public class DiscardEvent {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "productID")
    private long productID;

    @Nullable
    @ColumnInfo(name = "userID")
    private Long userID;

    @ColumnInfo(name = "quantity")
    private int quantity;

    @Nullable
    @ColumnInfo(name = "reason")
    private String reason;

    @Nullable
    @ColumnInfo(name = "createdAt")
    private LocalDate createdAt;

    public DiscardEvent(long productID, @Nullable Long userID, int quantity, @Nullable String reason, @Nullable LocalDate createdAt) {
        this.productID = productID;
        this.userID = userID;
        this.quantity = quantity;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProductID() { return productID; }
    public void setProductID(long productID) { this.productID = productID; }

    @Nullable
    public Long getUserID() { return userID; }
    public void setUserID(@Nullable Long userID) { this.userID = userID; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Nullable
    public String getReason() { return reason; }
    public void setReason(@Nullable String reason) { this.reason = reason; }

    @Nullable
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(@Nullable LocalDate createdAt) { this.createdAt = createdAt; }
}