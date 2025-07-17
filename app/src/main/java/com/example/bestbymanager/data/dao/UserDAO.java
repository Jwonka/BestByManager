package com.example.bestbymanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.example.bestbymanager.data.entities.User;
import com.example.bestbymanager.data.pojo.UserReportRow;
import com.example.bestbymanager.utilities.PasswordUtil;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Dao
public interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM USER WHERE userName = :username LIMIT 1")
    User findByUsername(String username);

    @Query("SELECT * FROM USER WHERE userID = :userID LIMIT 1")
    LiveData<User> findByUserID(long userID);

    @Query("SELECT COUNT(*) FROM USER")
    int userCount();

    @Query("UPDATE user SET isAdmin = :adminFlag WHERE userID = :userID")
    void setAdminFlag(long userID, boolean adminFlag);

    @Query("SELECT * FROM user WHERE userID = :userID LIMIT 1")
    LiveData<User> getUser(long userID);

    @Query("SELECT * FROM user ORDER BY userID")
    LiveData<List<User>> getUsers();

    @Transaction
    default String setTempPassword(long userId) {
        String tmp = PasswordUtil.generateTempPassword();
        String hash = PasswordUtil.hash(tmp);
        OffsetDateTime expires = OffsetDateTime.now().plusHours(24);
        updatePassword(userId, hash, expires, true);
        return tmp;
    }

    @Query("UPDATE `user` SET hash = :hash, mustChange = :mustChange, resetExpires = :expires WHERE userID = :id")
    void updatePassword(long id, String hash, OffsetDateTime expires, boolean mustChange);

    @Query("UPDATE `user` SET hash = :hash, mustChange = 0, resetTokenHash = NULL, resetExpires = NULL WHERE userID = :id")
    void changePassword(long id, String hash);

    @Query("SELECT * FROM user WHERE isAdmin = 1")
    LiveData<List<User>> loadAdmins();

    @Query("SELECT userID, userName, firstName, lastName FROM user WHERE isAdmin = 1")
    LiveData<List<UserReportRow>> getAdmins();

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM    user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE   product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY totalCount DESC")
    LiveData<List<UserReportRow>> getEntriesByDateRange(LocalDate from, LocalDate to, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM    user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE   product.barcode = :barcode " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY totalCount DESC")
    LiveData<List<UserReportRow>> getEntriesByBarcode(String barcode, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM    user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE product.barcode = :barcode AND user.userID = :userID " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY product.expirationDate ASC")
    LiveData<List<UserReportRow>> getEntriesForEmployeeAndBarcode(long userID, String barcode, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM    user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE user.userID = :userID AND product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY product.expirationDate ASC")
    LiveData<List<UserReportRow>> getEntriesForEmployeeInRange(long userID, LocalDate from, LocalDate to, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE product.barcode = :barcode AND user.userID = :userID AND product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY MIN(product.expirationDate)")
    LiveData<List<UserReportRow>> getEntriesByBarcodeForEmployeeInRange(long userID, String barcode, LocalDate from, LocalDate to, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM    user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE product.barcode = :barcode AND product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY product.expirationDate ASC")
    LiveData<List<UserReportRow>> getEntriesByBarcodeForRange(String barcode, LocalDate from, LocalDate to, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM    user user " +
            "JOIN    product product  ON product.userID = user.userID " +
            "WHERE user.userID = :userID " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY product.expirationDate ASC")
    LiveData<List<UserReportRow>> getEntriesByEmployee(long userID, LocalDate today);

    @Query("SELECT user.userID AS userID, " +
            "user.userName AS userName, " +
            "user.firstName AS firstName, " +
            "user.lastName AS lastName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "SUM(CASE WHEN product.expirationDate < :today THEN 1 ELSE 0 END) AS expiredCount, " +
            "SUM(CASE WHEN product.expirationDate >= :today THEN 1 ELSE 0 END) AS goodCount, " +
            "COUNT(*) AS totalCount " +
            "FROM user user " +
            "JOIN product product ON product.userID = user.userID " +
            "GROUP BY user.userID, user.userName, user.firstName, user.lastName, product.brand, product.productName " +
            "ORDER BY user.userID ASC")
    LiveData<List<UserReportRow>> getAllEntries(LocalDate today);
}
