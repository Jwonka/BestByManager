package com.bestbymanager.app.data.database;

import android.content.Context;
import androidx.annotation.NonNull;
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

@Database(entities = {User.class, Product.class, DiscardEvent.class}, version = 21, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ProductDatabaseBuilder extends RoomDatabase {

    public abstract UserDAO userDAO();
    public abstract ProductDAO productDAO();
    public static final String DB_NAME = "MyProductDatabase.db";
    private static volatile ProductDatabaseBuilder INSTANCE;

    public static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
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
        public void migrate(@NonNull SupportSQLiteDatabase db) {
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

    public static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // remap old category indices to new indices
            db.execSQL(
                    "UPDATE product SET category = CASE category " +
                            "WHEN 0 THEN 0 " +
                            "WHEN 1 THEN 1 " +
                            "WHEN 2 THEN 2 " +
                            "WHEN 3 THEN 3 " +
                            "WHEN 4 THEN 6 " +   // Candy
                            "WHEN 5 THEN 14 " +  // Snacks
                            "WHEN 6 THEN 4 " +   // Canned Goods
                            "WHEN 7 THEN 11 " +  // Packaged Meals
                            "WHEN 8 THEN 7 " +   // Condiments
                            "WHEN 9 THEN 8 " +   // Dairy
                            "WHEN 10 THEN 9 " +  // Deli
                            "WHEN 11 THEN 10 " + // Frozen
                            "WHEN 12 THEN 12 " + // Produce
                            "WHEN 13 THEN 13 " + // Personal Care
                            "WHEN 14 THEN 15 " + // Tobacco
                            "ELSE category END"
            );
        }
    };

    public static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE product " +
                            "ADD COLUMN earlyWarningEnabled INTEGER NOT NULL DEFAULT 0"
            );
        }
    };

    public static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) { /* no-op */ }
    };

    public static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add kiosk-mode columns to user
            db.execSQL("ALTER TABLE `user` ADD COLUMN `isOwner` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `user` ADD COLUMN `employeePinHash` TEXT");
            db.execSQL("ALTER TABLE `user` ADD COLUMN `employeePinFailedAttempts` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `user` ADD COLUMN `employeePinLockedUntil` INTEGER");

            // Stamp the earliest existing admin as Owner (super admin)
            // If there are no admins, this will do nothing
            db.execSQL(
                    "UPDATE `user` " +
                            "SET `isOwner` = 1 " +
                            "WHERE `userID` = (" +
                            "  SELECT `userID` FROM `user` WHERE `isAdmin` = 1 ORDER BY `userID` ASC LIMIT 1" +
                            ")"
            );
        }
    };

    public static ProductDatabaseBuilder getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ProductDatabaseBuilder.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ProductDatabaseBuilder.class,
                                    DB_NAME
                            )
                            .setTransactionExecutor(Executors.newSingleThreadExecutor())
                            .setQueryExecutor(Executors.newFixedThreadPool(4))
                            .addMigrations(
                                    MIGRATION_15_16,
                                    MIGRATION_16_17,
                                    MIGRATION_17_18,
                                    MIGRATION_18_19,
                                    MIGRATION_19_20,
                                    MIGRATION_20_21
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}