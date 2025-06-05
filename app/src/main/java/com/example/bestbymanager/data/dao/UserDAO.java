package com.example.bestbymanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.bestbymanager.data.entities.User;

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
    LiveData<User> findByUserId(int userID);

    @Query("SELECT COUNT(*) FROM USER")
    int userCount();

    @Query("UPDATE user SET isAdmin = :adminFlag WHERE userID = :userId")
    void setAdminFlag(int userId, boolean adminFlag);
}
