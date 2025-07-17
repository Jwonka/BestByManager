package com.example.bestbymanager.data.database;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
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
                            .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                            .setTransactionExecutor(Executors.newSingleThreadExecutor())
                            .setQueryExecutor(Executors.newFixedThreadPool(4))
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {

            db.execSQL(
                    "CREATE TABLE user_new (" +
                            " userID          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            " userName        TEXT    NOT NULL," +
                            " firstName       TEXT    NOT NULL," +
                            " lastName        TEXT    NOT NULL," +
                            " hash            TEXT    NOT NULL," +
                            " thumbnail       BLOB," +
                            " isAdmin         INTEGER NOT NULL," +
                            " resetTokenHash  TEXT," +
                            " resetExpires    TEXT," +
                            " mustChange      INTEGER NOT NULL DEFAULT 0" +
                            ")");

            db.execSQL(
                    "INSERT INTO user_new (" +
                            " userID, userName, firstName, lastName, hash, thumbnail, isAdmin," +
                            " resetTokenHash, resetExpires, mustChange) " +
                            "SELECT userID, userName, firstName, lastName," +
                            "       /* if any NULL hash slipped through, set dummy */" +
                            "       COALESCE(hash, '')," +
                            "       thumbnail, isAdmin, resetTokenHash, resetExpires, mustChange " +
                            "FROM user");

            db.execSQL("DROP TABLE user");
            db.execSQL("ALTER TABLE user_new RENAME TO user");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_userName ON user(userName)");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {

            db.execSQL(
                    "CREATE TABLE user_new (" +
                            " userID          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            " userName        TEXT    NOT NULL," +
                            " firstName       TEXT    NOT NULL," +
                            " lastName        TEXT    NOT NULL," +
                            " hash            TEXT," +
                            " thumbnail       BLOB," +
                            " isAdmin         INTEGER NOT NULL," +
                            " resetTokenHash  TEXT," +
                            " resetExpires    TEXT," +
                            " mustChange      INTEGER NOT NULL DEFAULT 0" +
                            ")");

            db.execSQL(
                    "INSERT INTO user_new (" +
                            " userID, userName, firstName, lastName, hash, thumbnail, isAdmin," +
                            " resetTokenHash, resetExpires, mustChange) " +
                            "SELECT userID, userName, firstName, lastName, hash, thumbnail, isAdmin," +
                            "       resetTokenHash, resetExpires, mustChange " +   // may all be NULL
                            "FROM user");

            db.execSQL("DROP TABLE user");
            db.execSQL("ALTER TABLE user_new RENAME TO user");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_userName ON user(userName)");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE user ADD COLUMN resetTokenHash TEXT");
            db.execSQL("ALTER TABLE user ADD COLUMN resetExpires   TEXT");
            db.execSQL("ALTER TABLE user ADD COLUMN mustChange     INTEGER NOT NULL DEFAULT 0");
        }
    };
}
