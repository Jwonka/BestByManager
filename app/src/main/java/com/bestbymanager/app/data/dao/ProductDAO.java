package com.bestbymanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.bestbymanager.app.data.entities.DiscardEvent;
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

    @Query("SELECT earlyWarningEnabled FROM product WHERE productID = :productID LIMIT 1")
    int getEarlyWarningEnabledBlocking(long productID);

    @Query("SELECT quantity FROM product WHERE productID = :productID LIMIT 1")
    int getQuantityBlocking(long productID);

    @Query("UPDATE product SET earlyWarningEnabled = 0 WHERE productID = :productID")
    int clearEarlyWarning(long productID);

    @Query("SELECT * FROM product WHERE productID = :productID LIMIT 1")
    LiveData<Product> getProduct(long productID);

    @Query("SELECT * FROM product WHERE expirationDate >= :today ORDER BY expirationDate ASC, productName COLLATE NOCASE ASC, brand COLLATE NOCASE ASC, productID ASC")
    LiveData<List<Product>> getProducts(LocalDate today);

    @Query("SELECT * FROM product WHERE barcode = :barcode ORDER BY expirationDate ASC, productID ASC LIMIT 1")
    Product getRecentExpirationByBarcode(String barcode);

    @Query("SELECT * FROM product WHERE barcode = :barcode ORDER BY expirationDate ASC, productID ASC")
    LiveData<List<Product>> getProductsByBarcode(String barcode);

    @Query("SELECT * FROM product WHERE expirationDate BETWEEN :from AND :selected ORDER BY expirationDate ASC, productName COLLATE NOCASE ASC, brand COLLATE NOCASE ASC, productID ASC")
    LiveData<List<Product>> getProductsByDateRange(LocalDate from, LocalDate selected);

    @Insert
    long insertDiscardEvent(DiscardEvent event);

    @Query("UPDATE product SET quantity = quantity - :quantity " +
            "WHERE productID = :productID AND quantity >= :quantity")
    int decrementQuantity(long productID, int quantity);

    @Transaction
    default boolean discardProduct(DiscardEvent event) {
        if (event == null || event.getQuantity() <= 0) return false;

        int updated = decrementQuantity(event.getProductID(), event.getQuantity());
        if (updated == 0) return false;

        insertDiscardEvent(event);
        return true;
    }

    @Query("SELECT product.productID AS productID, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, " +
            "COALESCE((SELECT SUM(quantity) FROM discard_event WHERE discard_event.productID = product.productID), 0) AS discardedQuantity, " +
            "COALESCE((SELECT reason FROM discard_event de WHERE de.productID = product.productID AND de.reason IS NOT NULL AND de.reason != '' ORDER BY de.createdAt DESC, de.id DESC LIMIT 1), NULL) AS lastDiscardNote, " +
            "employee.employeeName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN employee ON employee.employeeID = product.employeeID " +
            "WHERE expirationDate < :cutoff " +
            "ORDER BY expirationDate DESC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC, product.productID ASC")
    LiveData<List<ProductReportRow>> getExpired(LocalDate cutoff);

    @Query("SELECT product.productID AS productID, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, " +
            "COALESCE((SELECT SUM(quantity) FROM discard_event WHERE discard_event.productID = product.productID), 0) AS discardedQuantity, " +
            "COALESCE((SELECT reason FROM discard_event de WHERE de.productID = product.productID AND de.reason IS NOT NULL AND de.reason != '' ORDER BY de.createdAt DESC, de.id DESC LIMIT 1), NULL) AS lastDiscardNote, " +
            "employee.employeeName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN employee ON employee.employeeID = product.employeeID " +
            "WHERE expirationDate BETWEEN :from AND :selected " +
            "ORDER BY expirationDate ASC, productName COLLATE NOCASE ASC, brand COLLATE NOCASE ASC, productID ASC")
    LiveData<List<ProductReportRow>> getExpiring(LocalDate from, LocalDate selected);

    @Query("SELECT product.productID AS productID, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, " +
            "COALESCE((SELECT SUM(quantity) FROM discard_event WHERE discard_event.productID = product.productID), 0) AS discardedQuantity, " +
            "COALESCE((SELECT reason FROM discard_event de WHERE de.productID = product.productID AND de.reason IS NOT NULL AND de.reason != '' ORDER BY de.createdAt DESC, de.id DESC LIMIT 1), NULL) AS lastDiscardNote, " +
            "employee.employeeName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN employee ON product.employeeID = employee.employeeID " +
            "ORDER BY product.expirationDate ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC, product.productID ASC")
    List<ProductReportRow> getReportRows();

    @Query("SELECT product.productID AS productID, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, " +
            "COALESCE((SELECT SUM(quantity) FROM discard_event WHERE discard_event.productID = product.productID), 0) AS discardedQuantity, " +
            "COALESCE((SELECT reason FROM discard_event de WHERE de.productID = product.productID AND de.reason IS NOT NULL AND de.reason != '' ORDER BY de.createdAt DESC, de.id DESC LIMIT 1), NULL) AS lastDiscardNote, " +
            "employee.employeeName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN employee ON employee.employeeID = product.employeeID " +
            "WHERE product.barcode = :barcode " +
            "ORDER BY product.expirationDate ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC, product.productID ASC")
    LiveData<List<ProductReportRow>> getReportRowsByBarcode(String barcode);

    @Query("SELECT product.productID, " +
            "product.brand, " +
            "product.productName, " +
            "product.expirationDate, " +
            "product.purchaseDate, " +
            "product.quantity, " +
            "COALESCE((SELECT SUM(quantity) FROM discard_event WHERE discard_event.productID = product.productID), 0) AS discardedQuantity, " +
            "COALESCE((SELECT reason FROM discard_event de WHERE de.productID = product.productID AND de.reason IS NOT NULL AND de.reason != '' ORDER BY de.createdAt DESC, de.id DESC LIMIT 1), NULL) AS lastDiscardNote, " +
            "employee.employeeName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN employee ON product.employeeID = employee.employeeID " +
            "WHERE barcode = :barcode " +
            "AND expirationDate BETWEEN :from AND :to " +
            "ORDER BY expirationDate ASC, productName COLLATE NOCASE ASC, brand COLLATE NOCASE ASC, productID ASC")
    LiveData<List<ProductReportRow>> getProductsByBarcodeAndDateRange(String barcode, LocalDate from, LocalDate to);

    @Query("SELECT product.productID AS productID, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "product.expirationDate AS expirationDate, " +
            "product.purchaseDate AS purchaseDate, " +
            "product.quantity AS quantity, " +
            "COALESCE((SELECT SUM(quantity) FROM discard_event WHERE discard_event.productID = product.productID), 0) AS discardedQuantity, " +
            "COALESCE((SELECT reason FROM discard_event de WHERE de.productID = product.productID AND de.reason IS NOT NULL AND de.reason != '' ORDER BY de.createdAt DESC, de.id DESC LIMIT 1), NULL) AS lastDiscardNote, " +
            "employee.employeeName AS enteredBy, " +
            "product.barcode AS barcode, " +
            "product.category AS category " +
            "FROM product " +
            "JOIN employee ON employee.employeeID = product.employeeID " +
            "ORDER BY product.expirationDate ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC, product.productID ASC")
    LiveData<List<ProductReportRow>> getAllProducts();

    @Query("SELECT * " +
            "FROM product " +
            "WHERE employeeID = :employeeId AND barcode = :barcode " +
            "ORDER BY expirationDate DESC, productID DESC " +
            "LIMIT 1")
    Product getLatestByBarcodeForEmployee(long employeeId, String barcode);
}