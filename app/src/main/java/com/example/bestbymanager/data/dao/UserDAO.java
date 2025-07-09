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
import com.example.bestbymanager.utilities.PasswordUtil;
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
        updatePassword(userId, hash, true);
        return tmp;
    }

    @Query("UPDATE `user` SET hash = :hash, mustChange = :mustChange WHERE userID = :id")
    void updatePassword(long id, String hash, boolean mustChange);
}
