package com.example.bestbymanager.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteException;
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

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE user ADD COLUMN firstName TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE user ADD COLUMN lastName  TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE user ADD COLUMN thumbnail BLOB");
        }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {

            try {
                db.execSQL("ALTER TABLE product ADD COLUMN userID INTEGER NOT NULL DEFAULT 1");
            } catch (SQLiteException dup) { /* column already present – fine */ }

            db.execSQL(
                    "CREATE TABLE product_new (" +
                            "  productID      INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "  userID         INTEGER NOT NULL DEFAULT 1,"  +        // FK column
                            "  brand          TEXT," +
                            "  productName    TEXT NOT NULL," +
                            "  expirationDate INTEGER NOT NULL," +
                            "  quantity       INTEGER NOT NULL," +
                            "  weight         TEXT," +
                            "  barcode        TEXT," +
                            "  category       INTEGER NOT NULL," +
                            "  isle           INTEGER NOT NULL," +
                            "  purchaseDate   INTEGER," +
                            "  imageUri       TEXT," +
                            "  thumbnail      BLOB," +
                            "  FOREIGN KEY(userID) REFERENCES user(userID) ON DELETE CASCADE)"
            );

            db.execSQL(
                    "INSERT INTO product_new (" +
                            " productID, userID, brand, productName, expirationDate, quantity," +
                            " weight, barcode, category, isle, purchaseDate, imageUri, thumbnail)" +
                            " SELECT productID, userID, brand, productName, expirationDate, quantity," +
                            "        weight, barcode, category, isle, purchaseDate, imageUri, thumbnail" +
                            " FROM product"
            );

            db.execSQL("DROP TABLE product");
            db.execSQL("ALTER TABLE product_new RENAME TO product");

            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_userID ON product(userID)");

            db.execSQL("INSERT OR IGNORE INTO `user` (userID,userName,hash,isAdmin) VALUES(1,'Unknown','',0)");

            db.execSQL("PRAGMA foreign_keys = ON");
        }
    };
}
