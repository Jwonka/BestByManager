package com.bestbymanager.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;
import androidx.room.Update;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.data.pojo.EmployeeReportRow;
import com.bestbymanager.app.utilities.PasswordUtil;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Dao
public interface EmployeeDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Employee employee);

    @Update
    void update(Employee employee);

    @Delete
    void delete(Employee employee);

    @Query("SELECT * FROM employee WHERE employeeName = :name COLLATE NOCASE LIMIT 1")
    Employee findByName(String name);

    @Query("SELECT * FROM employee WHERE employeeID = :employeeID LIMIT 1")
    LiveData<Employee> findByEmployeeID(long employeeID);

    @Query("SELECT COUNT(*) FROM employee")
    int employeeCount();

    @Query("UPDATE employee SET isAdmin = :adminFlag WHERE employeeID = :employeeID")
    void setAdminFlag(long employeeID, boolean adminFlag);

    @Query("SELECT * FROM employee WHERE employeeID = :employeeID LIMIT 1")
    LiveData<Employee> getEmployee(long employeeID);

    @Query("SELECT * FROM employee ORDER BY employeeName COLLATE NOCASE ASC, employeeID ASC")
    LiveData<List<Employee>> getEmployees();

    @Query("SELECT employeeID FROM employee WHERE isAdmin = 1 ORDER BY employeeID ASC LIMIT 1")
    Long getFirstAdminId();

    @Transaction
    default String setTempPassword(long employeeID) {
        String tmp = PasswordUtil.generateTempPassword();
        String hash = PasswordUtil.hash(tmp);
        OffsetDateTime expires = OffsetDateTime.now().plusHours(24);
        updatePassword(employeeID, hash, expires, true);
        return tmp;
    }

    @Query("UPDATE employee SET hash = :hash, mustChange = :mustChange, resetExpires = :expires WHERE employeeID = :id")
    void updatePassword(long id, String hash, OffsetDateTime expires, boolean mustChange);

    @Query("UPDATE employee SET hash = :hash, mustChange = 0, resetTokenHash = NULL, resetExpires = NULL WHERE employeeID = :id")
    void changePassword(long id, String hash);

    // pin state read for selection flow
    @Query("SELECT employeePinHash FROM employee WHERE employeeID = :employeeId LIMIT 1")
    String getEmployeePinHashBlocking(long employeeId);

    @Query("SELECT employeePinFailedAttempts FROM employee WHERE employeeID = :employeeId LIMIT 1")
    int getEmployeePinFailedAttemptsBlocking(long employeeId);

    @Query("SELECT employeePinLockedUntil FROM employee WHERE employeeID = :employeeId LIMIT 1")
    Long getEmployeePinLockedUntilBlocking(long employeeId);

    @Query("UPDATE employee SET employeePinHash = :hash, employeePinFailedAttempts = 0, employeePinLockedUntil = NULL WHERE employeeID = :employeeId")
    void setEmployeePinHash(long employeeId, String hash);

    @Query("UPDATE employee SET employeePinFailedAttempts = 0, employeePinLockedUntil = NULL WHERE employeeID = :employeeId")
    void clearEmployeePinLockout(long employeeId);

    @Query("UPDATE employee SET employeePinFailedAttempts = employeePinFailedAttempts + 1 WHERE employeeID = :employeeId")
    void incrementEmployeePinFailedAttempts(long employeeId);

    @Query("UPDATE employee SET employeePinLockedUntil = :lockedUntil WHERE employeeID = :employeeId")
    void setEmployeePinLockedUntil(long employeeId, Long lockedUntil);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName " +
            "             AND de.createdAt BETWEEN :from AND :to), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName " +
            "            AND de.createdAt BETWEEN :from AND :to), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY employee.employeeName COLLATE NOCASE ASC, MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesByDateRange(LocalDate from, LocalDate to, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName " +
            "             AND p2.barcode = :barcode), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName " +
            "            AND p2.barcode = :barcode), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE product.barcode = :barcode " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY employee.employeeName COLLATE NOCASE ASC, MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesByBarcode(String barcode, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName " +
            "             AND p2.barcode = :barcode), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName " +
            "            AND p2.barcode = :barcode), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE product.barcode = :barcode AND employee.employeeID = :employeeID " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesForEmployeeAndBarcode(long employeeID, String barcode, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName " +
            "             AND de.createdAt BETWEEN :from AND :to), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName " +
            "            AND de.createdAt BETWEEN :from AND :to), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE employee.employeeID = :employeeID AND product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesForEmployeeInRange(long employeeID, LocalDate from, LocalDate to, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName " +
            "             AND p2.barcode = :barcode " +
            "             AND de.createdAt BETWEEN :from AND :to), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName " +
            "            AND p2.barcode = :barcode " +
            "            AND de.createdAt BETWEEN :from AND :to), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE product.barcode = :barcode AND employee.employeeID = :employeeID AND product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesByBarcodeForEmployeeInRange(long employeeID, String barcode, LocalDate from, LocalDate to, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName " +
            "             AND p2.barcode = :barcode " +
            "             AND de.createdAt BETWEEN :from AND :to), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName " +
            "            AND p2.barcode = :barcode " +
            "            AND de.createdAt BETWEEN :from AND :to), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE product.barcode = :barcode AND product.expirationDate BETWEEN :from AND :to " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY employee.employeeName COLLATE NOCASE ASC, MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesByBarcodeForRange(String barcode, LocalDate from, LocalDate to, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "WHERE employee.employeeID = :employeeID " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getEntriesByEmployee(long employeeID, LocalDate today);

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT employee.employeeID AS employeeID, " +
            "employee.employeeName AS employeeName, " +
            "product.brand AS brand, " +
            "product.productName AS productName, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate < :today THEN product.quantity ELSE 0 END), 0) AS expiredCount, " +
            "COALESCE(SUM(CASE WHEN product.expirationDate >= :today THEN product.quantity ELSE 0 END), 0) AS goodCount, " +
            "COUNT(*) AS lotCount, " +
            "(COALESCE(SUM(product.quantity), 0) + " +
            " COALESCE((SELECT SUM(de.quantity) " +
            "           FROM discard_event de " +
            "           JOIN product p2 ON p2.productID = de.productID " +
            "           WHERE p2.employeeID = employee.employeeID " +
            "             AND p2.brand = product.brand " +
            "             AND p2.productName = product.productName), 0)) AS totalCount, " +
            "COALESCE((SELECT SUM(de.quantity) " +
            "          FROM discard_event de " +
            "          JOIN product p2 ON p2.productID = de.productID " +
            "          WHERE p2.employeeID = employee.employeeID " +
            "            AND p2.brand = product.brand " +
            "            AND p2.productName = product.productName), 0) AS discardedCount, " +
            "COALESCE(( " +
            "  SELECT de.reason " +
            "  FROM discard_event de " +
            "  JOIN product p2 ON p2.productID = de.productID " +
            "  WHERE p2.employeeID = employee.employeeID " +
            "    AND p2.brand = product.brand " +
            "    AND p2.productName = product.productName " +
            "    AND de.reason IS NOT NULL AND de.reason != '' " +
            "  ORDER BY de.createdAt DESC, de.id DESC " +
            "  LIMIT 1 " +
            "), NULL) AS lastDiscardNote " +
            "FROM employee employee " +
            "JOIN product product ON product.employeeID = employee.employeeID " +
            "GROUP BY employee.employeeID, employee.employeeName, product.brand, product.productName " +
            "ORDER BY employee.employeeName COLLATE NOCASE ASC, MIN(product.expirationDate) ASC, product.productName COLLATE NOCASE ASC, product.brand COLLATE NOCASE ASC")
    LiveData<List<EmployeeReportRow>> getAllEntries(LocalDate today);
}