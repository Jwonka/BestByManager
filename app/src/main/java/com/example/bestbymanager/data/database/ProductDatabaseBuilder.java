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

@Database(entities = {User.class, Product.class}, version = 11, exportSchema = false)
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
                            .addMigrations(MIGRATION_10_11)
                            .setTransactionExecutor(Executors.newSingleThreadExecutor())
                            .setQueryExecutor(Executors.newFixedThreadPool(4))
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {

            // 1. add the column so the SELECT below can read it
            try {
                db.execSQL("ALTER TABLE product ADD COLUMN userID INTEGER NOT NULL DEFAULT 1");
            } catch (SQLiteException dup) { /* column already present – fine */ }

            // 2. create new table with full FK + index
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

            // 3. copy all rows, giving legacy rows userID = 1
            db.execSQL(
                    "INSERT INTO product_new (" +
                            " productID, userID, brand, productName, expirationDate, quantity," +
                            " weight, barcode, category, isle, purchaseDate, imageUri, thumbnail)" +
                            " SELECT productID, userID, brand, productName, expirationDate, quantity," +
                            "        weight, barcode, category, isle, purchaseDate, imageUri, thumbnail" +
                            " FROM product"
            );

            // 4. replace old table
            db.execSQL("DROP TABLE product");
            db.execSQL("ALTER TABLE product_new RENAME TO product");

            // 5. index for fast joins
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_userID ON product(userID)");

            // 6. make sure sentinel user exists
            db.execSQL("INSERT OR IGNORE INTO `user`" +
                    "(userID,userName,hash,isAdmin) VALUES(1,'Unknown','',0)");

            db.execSQL("PRAGMA foreign_keys = ON");
        }
    };
}
