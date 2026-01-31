package com.bestbymanager.app.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.bestbymanager.app.data.dao.ProductDAO;
import com.bestbymanager.app.data.dao.UserDAO;
import com.bestbymanager.app.data.entities.DiscardEvent;
import com.bestbymanager.app.data.entities.Product;
import com.bestbymanager.app.data.entities.User;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Product.class, DiscardEvent.class}, version = 17, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ProductDatabaseBuilder extends RoomDatabase {

    public abstract UserDAO userDAO();
    public abstract ProductDAO productDAO();

    private static volatile ProductDatabaseBuilder INSTANCE;

    public static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `discard_event` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`productID` INTEGER NOT NULL, " +
                    "`userID` INTEGER, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "`reason` TEXT, " +
                    "`createdAt` INTEGER, " +
                    "FOREIGN KEY(`productID`) REFERENCES `product`(`productID`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`userID`) REFERENCES `user`(`userID`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discard_event_productID` ON `discard_event` (`productID`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discard_event_userID` ON `discard_event` (`userID`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_discard_event_createdAt` ON `discard_event` (`createdAt`)");

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_expirationDate_brand` ON `product` (`expirationDate`, `brand`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_barcode_expirationDate` ON `product` (`barcode`, `expirationDate`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_userID_expirationDate` ON `product` (`userID`, `expirationDate`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_productName_expirationDate` ON `product` (`productName`, `expirationDate`)");
        }
    };

    public static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("PRAGMA foreign_keys=OFF");
            db.beginTransaction();
            try {
                db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `product_new` (" +
                                "`productID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                                "`userID` INTEGER NOT NULL," +
                                "`brand` TEXT," +
                                "`productName` TEXT NOT NULL," +
                                "`expirationDate` INTEGER NOT NULL," +
                                "`quantity` INTEGER NOT NULL," +
                                "`weight` TEXT," +
                                "`barcode` TEXT," +
                                "`category` INTEGER NOT NULL," +
                                "`isle` INTEGER NOT NULL," +
                                "`purchaseDate` INTEGER," +
                                "`imageUri` TEXT," +
                                "`thumbnail` BLOB," +
                                "FOREIGN KEY(`userID`) REFERENCES `user`(`userID`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                                ")"
                );

                db.execSQL(
                        "INSERT INTO `product_new` (" +
                                "`productID`,`userID`,`brand`,`productName`,`expirationDate`,`quantity`,`weight`,`barcode`,`category`,`isle`,`purchaseDate`,`imageUri`,`thumbnail`" +
                                ") " +
                                "SELECT " +
                                "productID, userID, brand, productName, " +
                                "CASE " +
                                "WHEN typeof(expirationDate)='text' THEN CAST(julianday(expirationDate) - 2440587.5 AS INTEGER) " +
                                "ELSE expirationDate " +
                                "END, " +
                                "quantity, weight, barcode, category, isle, " +
                                "CASE " +
                                "WHEN purchaseDate IS NULL THEN NULL " +
                                "WHEN typeof(purchaseDate)='text' THEN CAST(julianday(purchaseDate) - 2440587.5 AS INTEGER) " +
                                "ELSE purchaseDate " +
                                "END, " +
                                "imageUri, thumbnail " +
                                "FROM `product`"
                );

                db.execSQL("DROP TABLE `product`");
                db.execSQL("ALTER TABLE `product_new` RENAME TO `product`");

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_userID` ON `product` (`userID`)");
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_expirationDate_brand` ON `product` (`expirationDate`, `brand`)");
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_barcode_expirationDate` ON `product` (`barcode`, `expirationDate`)");
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_userID_expirationDate` ON `product` (`userID`, `expirationDate`)");
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_productName_expirationDate` ON `product` (`productName`, `expirationDate`)");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                db.execSQL("PRAGMA foreign_keys=ON");
                db.execSQL("PRAGMA foreign_key_check");
            }
        }
    };
    public static ProductDatabaseBuilder getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ProductDatabaseBuilder.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ProductDatabaseBuilder.class,
                                    "MyProductDatabase.db"
                            )
                            .setTransactionExecutor(Executors.newSingleThreadExecutor())
                            .setQueryExecutor(Executors.newFixedThreadPool(4))
                            .addMigrations(MIGRATION_15_16, MIGRATION_16_17)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}