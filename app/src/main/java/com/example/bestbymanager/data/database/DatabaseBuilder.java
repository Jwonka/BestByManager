package com.example.bestbymanager.data.database;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.bestbymanager.data.dao.UserDAO;
import com.example.bestbymanager.data.entities.User;
import java.util.concurrent.Executors;

@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class DatabaseBuilder extends RoomDatabase {
    public abstract UserDAO userDAO();
    private static volatile DatabaseBuilder INSTANCE;
    public static DatabaseBuilder getDatabase(final Context context) {
        if (INSTANCE==null) {
            synchronized (DatabaseBuilder.class){
                if(INSTANCE==null) {
                    Log.d("Database", "Creating new database instance");
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), DatabaseBuilder.class, "MyDatabase.db")
                            .fallbackToDestructiveMigration()
                            .setTransactionExecutor(Executors.newSingleThreadExecutor())
                            .setQueryExecutor(Executors.newFixedThreadPool(4))
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
