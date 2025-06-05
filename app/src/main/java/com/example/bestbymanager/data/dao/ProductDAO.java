package com.example.bestbymanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.pojo.ExpiredProductReportRow;
import java.util.List;

@Dao
public interface ProductDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Product product);

    @Query("UPDATE PRODUCT SET productName = :name, expirationDate = :exp WHERE productID = :id")
    int updateProduct(int id, String name, String exp);

    @Query("DELETE FROM PRODUCT WHERE productID = :id")
    int deleteProduct(int id);

    @Query("SELECT * FROM PRODUCT WHERE productID = :productID")
    LiveData<Product> getProduct(int productID);

    @Query("SELECT * FROM PRODUCT ORDER BY productID ASC")
    LiveData<List<Product>> getProducts();

    @Transaction
    @Query("SELECT p.productID AS prod_productID, " +
            "p.productName AS prod_productName, " +
            "p.expirationDate AS prod_expirationDate,  " +
            "COUNT(productID) AS expiredCount " +
            "FROM PRODUCT p " +
            "WHERE p.expirationDate >= :date " +
            "GROUP BY p.productID " +
            "ORDER BY p.expirationDate ")
    LiveData<List<ExpiredProductReportRow>> reportRows(String date);
}

