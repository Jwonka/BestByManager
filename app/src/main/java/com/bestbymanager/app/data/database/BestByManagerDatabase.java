package com.bestbymanager.app.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.bestbymanager.app.data.dao.EmployeeDAO;
import com.bestbymanager.app.data.dao.ProductDAO;
import com.bestbymanager.app.data.entities.DiscardEvent;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.data.entities.Product;

@Database(entities = {Employee.class, Product.class, DiscardEvent.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class BestByManagerDatabase extends RoomDatabase {

    public abstract EmployeeDAO employeeDAO();
    public abstract ProductDAO productDAO();
    public static final String DB_NAME = "bestbymanager.db";
    private static volatile BestByManagerDatabase INSTANCE;

    public static BestByManagerDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BestByManagerDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BestByManagerDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}