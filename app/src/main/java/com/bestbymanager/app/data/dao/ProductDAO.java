package com.bestbymanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.bestbymanager.app.data.entities.Product;
import com.bestbymanager.app.data.pojo.ProductReportRow;
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

    @Query("SELECT * FROM product WHERE expirationDate >= :today ORDER BY expirationDate ASC")
    LiveData<List<Product>> getProducts(LocalDate today);

    @Query("SELECT * FROM product WHERE barcode = :barcode ORDER BY expirationDate ASC LIMIT 1")
    Product getRecentExpirationByBarcode(String barcode);

    @Query("SELECT * FROM product WHERE barcode = :barcode ORDER BY expirationDate ASC")
    LiveData<List<Product>> getProductsByBarcode(String barcode);

    @Query("SELECT * FROM product WHERE expirationDate BETWEEN :from AND :selected ORDER BY expirationDate ASC")
    LiveData<List<Product>> getProductsByDateRange(LocalDate from, LocalDate selected);

    @Query("SELECT product.productID, " +
            "product.brand, " +
            "product.productName, " +
            "product.expirationDate, " +
            "product.purchaseDate, " +
            "product.quantity, " +
            "user.userName AS enteredBy " +
            "FROM product " +
            "JOIN user ON user.userID = product.userID " +
            "WHERE expirationDate < :cutoff " +
            "ORDER BY expirationDate DESC, brand")
    LiveData<List<ProductReportRow>> getExpired(LocalDate cutoff);

    @Query("SELECT product.productId AS productID, "  +
            "product.brand AS brand, "      +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, "   +
            "user.userName AS enteredBy, "   +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN user ON user.userID = product.userID " +
            "WHERE expirationDate BETWEEN :from AND :selected " +
            "ORDER BY expirationDate ASC, brand")
    LiveData<List<ProductReportRow>> getExpiring(LocalDate from, LocalDate selected);
    @Query("SELECT product.productId AS productID, "  +
                    "product.brand AS brand, "      +
                    "product.productName AS productName, " +
                    "product.expirationDate AS expirationDate, " +
                    "product.purchaseDate AS purchaseDate, " +
                    "product.quantity AS quantity, "   +
                    "user.userName AS enteredBy, "   +
                    "product.barcode AS barcode, " +
                    "product.category AS category " +
                    "FROM product " +
                    "JOIN user ON product.userID = user.userID " +
                    "ORDER BY product.expirationDate ASC")
    List<ProductReportRow> getReportRows();

    @Query("SELECT product.productId AS productID, "  +
            "product.brand AS brand, "      +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, "   +
            "user.userName AS enteredBy, "   +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN user ON user.userID = product.userID " +
            "WHERE product.barcode = :barcode " +
            "ORDER BY product.expirationDate")
    LiveData<List<ProductReportRow>> getReportRowsByBarcode(String barcode);

    @Query("SELECT product.productID, " +
            "product.brand, " +
            "product.productName, " +
            "product.expirationDate, " +
            "product.purchaseDate, " +
            "product.quantity, " +
            "user.userName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN   user ON product.userID = user.userID " +
            "WHERE barcode = :barcode " +
            "AND expirationDate BETWEEN :from AND :to " +
            "ORDER BY expirationDate ASC")
    LiveData<List<ProductReportRow>> getProductsByBarcodeAndDateRange(String barcode, LocalDate from, LocalDate to);

    @Query("SELECT product.productId AS productID, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, " +
            "user.userName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN user ON user.userID = product.userID " +
            "ORDER BY product.productName ASC, product.expirationDate ASC")
    LiveData<List<ProductReportRow>> getAllProducts();
}

