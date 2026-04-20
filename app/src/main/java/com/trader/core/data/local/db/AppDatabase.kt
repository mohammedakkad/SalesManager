package com.trader.core.data.local.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.PaymentType

@Database(
    entities = [
        CustomerEntity::class,
        TransactionEntity::class,
        PaymentMethodEntity::class,
        PendingMessageEntity::class,
        ProductEntity::class,
        ProductUnitEntity::class,
        StockMovementEntity::class,
        InvoiceItemEntity::class,
        InventorySessionEntity::class,
        InventorySessionItemEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun pendingMessageDao(): PendingMessageDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        const val DB_NAME = "sales_manager.db"

        // ===================== MAIGRATIONS =====================
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_messages (
                        tempId     TEXT    NOT NULL PRIMARY KEY,
                        merchantId TEXT    NOT NULL,
                        text       TEXT    NOT NULL,
                        senderName TEXT    NOT NULL,
                        createdAt  INTEGER NOT NULL,
                        isFailed   INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentType TEXT NOT NULL DEFAULT 'DEBT'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN hasItems INTEGER NOT NULL DEFAULT 0")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS products (
                        id          TEXT NOT NULL PRIMARY KEY,
                        barcode     TEXT,
                        name        TEXT NOT NULL,
                        category    TEXT NOT NULL DEFAULT '',
                        imageUri    TEXT,
                        merchantId  TEXT NOT NULL,
                        createdAt   INTEGER NOT NULL,
                        updatedAt   INTEGER NOT NULL,
                        syncStatus  TEXT NOT NULL DEFAULT 'PENDING'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_units (
                        id                TEXT NOT NULL PRIMARY KEY,
                        productId         TEXT NOT NULL,
                        unitType          TEXT NOT NULL,
                        unitLabel         TEXT NOT NULL,
                        price             REAL NOT NULL DEFAULT 0,
                        quantityInStock   REAL NOT NULL DEFAULT 0,
                        itemsPerCarton    INTEGER,
                        lowStockThreshold REAL NOT NULL DEFAULT 0,
                        isDefault         INTEGER NOT NULL DEFAULT 0,
                        createdAt         INTEGER NOT NULL,
                        updatedAt         INTEGER NOT NULL,
                        syncStatus        TEXT NOT NULL DEFAULT 'PENDING',
                        FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pu_productId ON product_units(productId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_movements (
                        id                    TEXT NOT NULL PRIMARY KEY,
                        productId             TEXT NOT NULL,
                        productName           TEXT NOT NULL,
                        unitId                TEXT NOT NULL,
                        unitLabel             TEXT NOT NULL,
                        movementType          TEXT NOT NULL,
                        quantity              REAL NOT NULL,
                        quantityBefore        REAL NOT NULL,
                        quantityAfter         REAL NOT NULL,
                        relatedTransactionId  INTEGER,
                        note                  TEXT NOT NULL DEFAULT '',
                        merchantId            TEXT NOT NULL,
                        createdAt             INTEGER NOT NULL,
                        syncStatus            TEXT NOT NULL DEFAULT 'PENDING'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS invoice_items (
                        id            TEXT NOT NULL PRIMARY KEY,
                        transactionId INTEGER NOT NULL,
                        productId     TEXT NOT NULL,
                        productName   TEXT NOT NULL,
                        unitId        TEXT NOT NULL,
                        unitLabel     TEXT NOT NULL,
                        quantity      REAL NOT NULL,
                        pricePerUnit  REAL NOT NULL,
                        totalPrice    REAL NOT NULL,
                        merchantId    TEXT NOT NULL,
                        syncStatus    TEXT NOT NULL DEFAULT 'PENDING'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_sessions (
                        id                TEXT NOT NULL PRIMARY KEY,
                        merchantId        TEXT NOT NULL,
                        status            TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                        startedAt         INTEGER NOT NULL,
                        finishedAt        INTEGER,
                        totalAdjustments  INTEGER NOT NULL DEFAULT 0,
                        syncStatus        TEXT NOT NULL DEFAULT 'PENDING'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_session_items (
                        id              TEXT NOT NULL PRIMARY KEY,
                        sessionId       TEXT NOT NULL,
                        productId       TEXT NOT NULL,
                        productName     TEXT NOT NULL,
                        unitId          TEXT NOT NULL,
                        unitLabel       TEXT NOT NULL,
                        systemQuantity  REAL NOT NULL,
                        actualQuantity  REAL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS idx_pu_productId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_units_productId ON product_units(productId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_units ADD COLUMN weightUnit TEXT NOT NULL DEFAULT 'KG'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_units RENAME TO product_units_old")
                db.execSQL("""
            CREATE TABLE product_units (
                id                TEXT NOT NULL PRIMARY KEY,
                productId         TEXT NOT NULL,
                unitType          TEXT NOT NULL,
                unitLabel         TEXT NOT NULL,
                price             REAL NOT NULL DEFAULT 0,
                quantityInStock   REAL NOT NULL DEFAULT 0,
                itemsPerCarton    INTEGER,
                lowStockThreshold REAL NOT NULL DEFAULT 0,
                isDefault         INTEGER NOT NULL DEFAULT 0,
                weightUnit        TEXT NOT NULL DEFAULT 'KG',
                createdAt         INTEGER NOT NULL,
                updatedAt         INTEGER NOT NULL,
                syncStatus        TEXT NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY (productId) REFERENCES products(id)
                ON DELETE CASCADE
                DEFERRABLE INITIALLY DEFERRED
            )
        """)
                db.execSQL("INSERT INTO product_units SELECT * FROM product_units_old")
                db.execSQL("DROP TABLE product_units_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_units_productId ON product_units(productId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_units RENAME TO product_units_old")
                db.execSQL("""
            CREATE TABLE product_units (
                id                TEXT NOT NULL PRIMARY KEY,
                productId         TEXT NOT NULL,
                unitType          TEXT NOT NULL,
                unitLabel         TEXT NOT NULL,
                price             REAL NOT NULL DEFAULT 0,
                quantityInStock   REAL NOT NULL DEFAULT 0,
                itemsPerCarton    INTEGER,
                lowStockThreshold REAL NOT NULL DEFAULT 0,
                isDefault         INTEGER NOT NULL DEFAULT 0,
                weightUnit        TEXT NOT NULL DEFAULT 'KG',
                createdAt         INTEGER NOT NULL,
                updatedAt         INTEGER NOT NULL,
                syncStatus        TEXT NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE
            )
        """)
                db.execSQL("INSERT INTO product_units SELECT * FROM product_units_old")
                db.execSQL("DROP TABLE product_units_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_units_productId ON product_units(productId)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            }
        }

        // ===================== BUILD DATABASE =====================
        fun build(context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
            MIGRATION_8_9)
        .addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                insertGuestCustomer(db)
                // إضافة طرق الدفع
                db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('كاش', '${PaymentType.CASH.name}')")
                db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('بنك', '${PaymentType.BANK.name}')")
                db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('محفظة', '${PaymentType.WALLET.name}')")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // هذا السطر هو الضمان الهندسي للمستخدمين القدامى
                insertGuestCustomer(db)
            }

            private fun insertGuestCustomer(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO customers (id, name, phone, createdAt) VALUES (-1, 'زبون زائر', '', 0)"
                )
            }
        })
        .build()
    }
}

suspend fun AppDatabase.upsertProductWithUnits(
    product: ProductEntity,
    units: List<ProductUnitEntity>
) {
    withTransaction {
        productDao().insertProduct(product) // المنتج ثانياً
        productDao().insertUnits(units) // الوحدات أولاً

    }
}

suspend fun AppDatabase.upsertProductWithUnitsAndClean(
    product: ProductEntity,
    units: List<ProductUnitEntity>
) {
    withTransaction {
        productDao().insertProduct(product) // ✅ المنتج أولاً
        productDao().insertUnits(units) // ✅ الوحدات ثانياً
        productDao().deleteRemovedUnits(product.id, units.map {
            it.id
        })
    }
}