package com.example.bestbymanager.data.database;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.bestbymanager.data.dao.ProductDAO;
import com.example.bestbymanager.data.dao.UserDAO;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.entities.User;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Product.class}, version = 15, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ProductDatabaseBuilder extends RoomDatabase {
    public abstract UserDAO userDAO();
    public abstract ProductDAO productDAO();
    private static volatile ProductDatabaseBuilder INSTANCE;
    public static ProductDatabaseBuilder getDatabase(final Context context) {
        if (INSTANCE==null) {
            synchronized (ProductDatabaseBuilder.class){
                if(INSTANCE==null) {
                    Log.d("ProductDatabase", "Creating new database instance");
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), ProductDatabaseBuilder.class, "MyProductDatabase.db")
                            .setTransactionExecutor(Executors.newSingleThreadExecutor())
                            .setQueryExecutor(Executors.newFixedThreadPool(4))
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
