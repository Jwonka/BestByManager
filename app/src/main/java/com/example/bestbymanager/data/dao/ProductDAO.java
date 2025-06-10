package com.example.bestbymanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.pojo.ExpiredProductReportRow;
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

    @Query("SELECT * FROM PRODUCT WHERE productID = :productID LIMIT 1")
    LiveData<Product> getProduct(int productID);

    @Query("SELECT * FROM PRODUCT ORDER BY expirationDate")
    LiveData<List<Product>> getProducts();

    @Query("SELECT * FROM product "
            + "WHERE expirationDate < :cutoff "
            + "ORDER BY expirationDate")
    LiveData<List<Product>> getExpired(LocalDate cutoff);

    @Query("SELECT * FROM product " +
            "WHERE expirationDate BETWEEN :today AND :selected " +
            "ORDER BY expirationDate")
    LiveData<List<Product>> getExpiringSoon(LocalDate today, LocalDate selected);

    @Transaction
    @Query("SELECT p.productID AS prod_productID, " +
            "p.productName AS prod_productName, " +
            "p.expirationDate AS prod_expirationDate,  " +
            "COUNT(productID) AS expiredCount " +
            "FROM PRODUCT p " +
            "WHERE p.expirationDate >= :from " +
            "GROUP BY p.productID " +
            "ORDER BY p.expirationDate ")
    LiveData<List<ExpiredProductReportRow>> reportRows(LocalDate from);
}

