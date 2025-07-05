package com.example.bestbymanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.pojo.ProductReportRow;
import java.time.LocalDate;
import java.util.List;

@Dao
public interface ProductDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Product product);

    @Update
    int updateProduct(Product product);

    @Delete
    int deleteProduct(Product product);

    @Query("SELECT * FROM product WHERE productID = :productID LIMIT 1")
    LiveData<Product> getProduct(long productID);

    @Query("SELECT * FROM product ORDER BY expirationDate ASC")
    LiveData<List<Product>> getProducts();

    @Query("SELECT product.productID, " +
            "      product.brand, " +
            "      product.productName, " +
            "      product.expirationDate, " +
            "      product.quantity, " +
            "      user.userName AS enteredBy " +
            "FROM product " +
            "JOIN user ON user.userID = product.userID " +
            "WHERE expirationDate < :cutoff " +
            "ORDER BY expirationDate, productName")
    LiveData<List<ProductReportRow>> getExpired(LocalDate cutoff);

    @Query("SELECT product.productID, " +
            "      product.brand, " +
            "      product.productName, " +
            "      product.expirationDate, " +
            "      product.quantity, " +
            "      user.userName AS enteredBy " +
            "FROM product " +
            "JOIN user ON user.userID = product.userID " +
            "WHERE expirationDate BETWEEN :from AND :selected " +
            "ORDER BY expirationDate, productName")
    LiveData<List<ProductReportRow>> getExpiring(LocalDate from, LocalDate selected);

    @Query("SELECT * FROM product WHERE barcode = :barcode ORDER BY expirationDate ASC LIMIT 1")
    Product getRecentExpirationByBarcode(String barcode);

    @Query("SELECT * FROM product WHERE barcode = :barcode ORDER BY expirationDate ASC")
    LiveData<List<Product>> getProductsByBarcode(String barcode);

    @Query("SELECT * FROM product WHERE expirationDate BETWEEN :from AND :selected ORDER BY expirationDate ASC")
    LiveData<List<Product>> getProductsByDateRange(LocalDate from, LocalDate selected);

    /** JOIN products→users to build the report rows */
    @Query(
            "SELECT product.productId         AS productID, "  +
                    "       product.brand             AS brand, "      +
                    "       product.productName       AS productName, " +
                    "       product.expirationDate    AS expirationDate, " +
                    "       product.quantity          AS quantity, "   +
                    "       user.userName          AS enteredBy "   +
                    "FROM   product " +
                    "JOIN   user ON product.userID = user.userID " +
                    "ORDER  BY product.expirationDate ASC")
    List<ProductReportRow> getReportRows();

    @Query("SELECT product.productID, product.brand, product.productName, " +
            "       product.expirationDate, product.quantity, user.userName AS enteredBy " +
            "FROM   product JOIN user ON user.userID = product.userID " +
            "WHERE  product.barcode = :barcode " +
            "ORDER  BY product.expirationDate")
    LiveData<List<ProductReportRow>> getReportRowsByBarcode(String barcode);
}

