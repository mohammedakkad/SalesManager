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
        InventorySessionItemEntity::class,
        ReturnInvoiceEntity::class,
        ReturnItemEntity::class,
        SessionEntity::class,
        EmployeeEntity::class,
        CashBoxEntity::class,
        CashBoxMovementEntity::class,
        LowStockAlertStateEntity::class
    ],
    version = 22,
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
    abstract fun returnDao(): ReturnDao
    abstract fun sessionDao(): SessionDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun cashBoxDao(): CashBoxDao
    abstract fun cashBoxMovementDao(): CashBoxMovementDao
    abstract fun lowStockAlertDao(): LowStockAlertDao

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
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_messages (
                        tempId     TEXT    NOT NULL PRIMARY KEY,
                        merchantId TEXT    NOT NULL,
                        text       TEXT    NOT NULL,
                        senderName TEXT    NOT NULL,
                        createdAt  INTEGER NOT NULL,
                        isFailed   INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentType TEXT NOT NULL DEFAULT 'DEBT'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN hasItems INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
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
                """.trimIndent()
                )

                db.execSQL(
                    """
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
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pu_productId ON product_units(productId)")

                db.execSQL(
                    """
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
                """.trimIndent()
                )

                db.execSQL(
                    """
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
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS inventory_sessions (
                        id                TEXT NOT NULL PRIMARY KEY,
                        merchantId        TEXT NOT NULL,
                        status            TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                        startedAt         INTEGER NOT NULL,
                        finishedAt        INTEGER,
                        totalAdjustments  INTEGER NOT NULL DEFAULT 0,
                        syncStatus        TEXT NOT NULL DEFAULT 'PENDING'
                    )
                """.trimIndent()
                )

                db.execSQL(
                    """
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
                """.trimIndent()
                )
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
                db.execSQL(
                    """
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
        """
                )
                db.execSQL("INSERT INTO product_units SELECT * FROM product_units_old")
                db.execSQL("DROP TABLE product_units_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_units_productId ON product_units(productId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_units RENAME TO product_units_old")
                db.execSQL(
                    """
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
        """
                )
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

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // الخطوة 1: إزالة الباركودات المكررة قبل إنشاء الـ UNIQUE INDEX
                // → نُبقي الصنف الأقدم (MIN rowid) ونُفرِّغ barcode البقية إلى NULL
                // → بدون هذه الخطوة: الأجهزة التي فيها تكرار تكرش عند فتح التطبيق
                db.execSQL(
                    """
                    UPDATE products
                    SET barcode = NULL
                    WHERE barcode IS NOT NULL
                      AND rowid NOT IN (
                          SELECT MIN(rowid)
                          FROM products
                          WHERE barcode IS NOT NULL
                          GROUP BY barcode, merchantId
                      )
                    """.trimIndent()
                )
                // الخطوة 2: إنشاء الـ UNIQUE INDEX بأمان بعد التنظيف
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_products_barcode_merchantId " +
                    "ON products (barcode, merchantId)"
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // إضافة عمود syncStatus لجدول customers
                // default 'SYNCED' حتى لا تُعامَل السجلات القديمة كـ PENDING
                db.execSQL(
                    "ALTER TABLE customers ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'"
                )
            }
        }


        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // جدول فواتير الإرجاع
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS return_invoices (
                        id                    TEXT NOT NULL PRIMARY KEY,
                        originalTransactionId INTEGER NOT NULL,
                        merchantId            TEXT NOT NULL,
                        returnType            TEXT NOT NULL DEFAULT 'FULL',
                        totalRefund           REAL NOT NULL DEFAULT 0,
                        note                  TEXT NOT NULL DEFAULT '',
                        createdAt             INTEGER NOT NULL,
                        syncStatus            TEXT NOT NULL DEFAULT 'PENDING'
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_invoices_originalTransactionId ON return_invoices(originalTransactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_invoices_merchantId ON return_invoices(merchantId)")

                // جدول أصناف الإرجاع
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS return_items (
                        id               TEXT NOT NULL PRIMARY KEY,
                        returnInvoiceId  TEXT NOT NULL,
                        productId        TEXT NOT NULL,
                        productName      TEXT NOT NULL,
                        unitId           TEXT NOT NULL,
                        unitLabel        TEXT NOT NULL,
                        originalQuantity REAL NOT NULL,
                        returnedQuantity REAL NOT NULL,
                        pricePerUnit     REAL NOT NULL,
                        costPricePerUnit REAL NOT NULL DEFAULT 0,
                        totalRefund      REAL NOT NULL DEFAULT 0,
                        lostProfit       REAL NOT NULL DEFAULT 0,
                        FOREIGN KEY (returnInvoiceId) REFERENCES return_invoices(id) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_items_returnInvoiceId ON return_items(returnInvoiceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_return_items_productId ON return_items(productId)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE product_units ADD COLUMN costPrice REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sessions (
                        id         TEXT NOT NULL PRIMARY KEY,
                        deviceId   TEXT NOT NULL,
                        deviceName TEXT NOT NULL,
                        loginDate  INTEGER NOT NULL,
                        lastActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_lastActive ON sessions(lastActive)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS employees (
                        id         TEXT NOT NULL PRIMARY KEY,
                        merchantId TEXT NOT NULL,
                        name       TEXT NOT NULL,
                        pinCode    TEXT NOT NULL,
                        role       TEXT NOT NULL,
                        createdAt  INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING'
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_employees_merchantId ON employees(merchantId)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // إضافة عمود deviceModel لجدول sessions
                // DEFAULT '' لضمان التوافق مع السجلات القديمة
                db.execSQL(
                    "ALTER TABLE sessions ADD COLUMN deviceModel TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // جدول الصناديق — صندوق رصيد لكل طريقة دفع (ربط 1:1)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cash_boxes (
                        id                  TEXT NOT NULL PRIMARY KEY,
                        paymentMethodId     INTEGER NOT NULL,
                        paymentMethodName   TEXT NOT NULL,
                        currentBalance      REAL NOT NULL DEFAULT 0,
                        initialBalance      REAL NOT NULL DEFAULT 0,
                        initialBalanceSetAt INTEGER,
                        merchantId          TEXT NOT NULL DEFAULT '',
                        updatedAt           INTEGER NOT NULL,
                        syncStatus          TEXT NOT NULL DEFAULT 'PENDING'
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cash_boxes_paymentMethodId ON cash_boxes(paymentMethodId)")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                purgeOrphanedInvoiceItems(db)
                if (!hasColumn(db, "transactions", "dueDate")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN dueDate INTEGER")
                }
                if (!hasColumn(db, "transactions", "reminderEnabled")) {
                    db.execSQL(
                        "ALTER TABLE transactions ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1"
                    )
                }
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildTransactionsTable(db)
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_isPaid_customerId " +
                        "ON transactions(isPaid, customerId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_invoice_items_transactionId " +
                        "ON invoice_items(transactionId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_invoice_items_productId " +
                        "ON invoice_items(productId)"
                )
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS low_stock_alert_states (
                        unitId TEXT NOT NULL PRIMARY KEY,
                        lastStatus TEXT NOT NULL,
                        lastNotifiedStatus TEXT,
                        lastNotifiedAt INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cash_box_movements (
                        id                    TEXT NOT NULL PRIMARY KEY,
                        cashBoxId             TEXT NOT NULL,
                        paymentMethodName     TEXT NOT NULL,
                        type                  TEXT NOT NULL,
                        amountDelta           REAL NOT NULL,
                        balanceAfter          REAL NOT NULL,
                        note                  TEXT NOT NULL DEFAULT '',
                        relatedTransactionId  INTEGER,
                        createdAt             INTEGER NOT NULL,
                        merchantId            TEXT NOT NULL DEFAULT '',
                        syncStatus            TEXT NOT NULL DEFAULT 'PENDING'
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_box_movements_cashBoxId ON cash_box_movements(cashBoxId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_box_movements_createdAt ON cash_box_movements(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_box_movements_syncStatus ON cash_box_movements(syncStatus)")
            }
        }

        // ===================== BUILD DATABASE =====================
        fun build(context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16, // ✅ deviceModel في sessions
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22
        )
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
                    "INSERT OR IGNORE INTO customers (id, name, phone, createdAt, syncStatus) " +
                    "VALUES (-1, 'زبون زائر', '', 0, 'SYNCED')"
                )
            }
        })
        .build()

        private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
            db.query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex < 0) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == column) return true
                }
            }
            return false
        }

        private fun purgeOrphanedInvoiceItems(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                DELETE FROM invoice_items
                WHERE transactionId NOT IN (SELECT id FROM transactions)
                """.trimIndent()
            )
        }

        private fun rebuildTransactionsTable(db: SupportSQLiteDatabase) {
            purgeOrphanedInvoiceItems(db)
            val hasDueDate = hasColumn(db, "transactions", "dueDate")
            val hasReminderEnabled = hasColumn(db, "transactions", "reminderEnabled")
            val dueDateExpr = if (hasDueDate) "dueDate" else "NULL"
            val reminderExpr = if (hasReminderEnabled) "reminderEnabled" else "1"

            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerId INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    isPaid INTEGER NOT NULL,
                    paymentMethodId INTEGER,
                    note TEXT NOT NULL DEFAULT '',
                    date INTEGER NOT NULL,
                    paidAt INTEGER,
                    paymentType TEXT NOT NULL DEFAULT 'DEBT',
                    hasItems INTEGER NOT NULL DEFAULT 0,
                    syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                    dueDate INTEGER,
                    reminderEnabled INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(customerId) REFERENCES customers(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions_new (
                    id, customerId, amount, isPaid, paymentMethodId, note, date,
                    paidAt, paymentType, hasItems, syncStatus, dueDate, reminderEnabled
                )
                SELECT
                    id, customerId, amount, isPaid, paymentMethodId, note, date,
                    paidAt, paymentType, hasItems, syncStatus, $dueDateExpr, $reminderExpr
                FROM transactions
                """.trimIndent()
            )
            db.execSQL("DROP TABLE transactions")
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_transactions_customerId ON transactions(customerId)"
            )
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'transactions'")
            db.execSQL(
                """
                INSERT INTO sqlite_sequence (name, seq)
                SELECT 'transactions', IFNULL(MAX(id), 0) FROM transactions
                """.trimIndent()
            )
            db.execSQL("PRAGMA foreign_keys=ON")
        }
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
        productDao().insertProduct(product)
        productDao().insertUnits(units)
        productDao().deleteRemovedUnits(product.id, units.map {
            it.id
        })
    }
}

suspend fun AppDatabase.recordCashBoxBalanceChange(
    boxId: String,
    delta: Double,
    absoluteBalance: Double?,
    updatedAt: Long,
    movement: CashBoxMovementEntity,
    markAsInitial: Boolean = false
) {
    withTransaction {
        when {
            markAsInitial && absoluteBalance != null ->
                cashBoxDao().setInitialBalanceFields(boxId, absoluteBalance, updatedAt, updatedAt)
            absoluteBalance != null ->
                cashBoxDao().updateBalance(boxId, absoluteBalance, updatedAt)
            delta != 0.0 ->
                cashBoxDao().applyDelta(boxId, delta, updatedAt)
        }
        val box = cashBoxDao().getById(boxId) ?: return@withTransaction
        cashBoxMovementDao().insert(
            movement.copy(balanceAfter = box.currentBalance)
        )
    }
}