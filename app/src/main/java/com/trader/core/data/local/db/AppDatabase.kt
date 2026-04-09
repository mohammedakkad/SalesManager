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
        PendingMessageEntity::class   // ← جديد
    ],
    version = 3,   // ← رُقِّي من 2 إلى 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun pendingMessageDao(): PendingMessageDao  // ← جديد

    companion object {
        const val DB_NAME = "sales_manager.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }

        // v2→v3: جدول الرسائل المعلقة (offline)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_messages (
                        tempId    TEXT    NOT NULL PRIMARY KEY,
                        merchantId TEXT   NOT NULL,
                        text      TEXT    NOT NULL,
                        senderName TEXT   NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isFailed  INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun build(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('كاش', '${PaymentType.CASH.name}')")
                        db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('بنك', '${PaymentType.BANK.name}')")
                        db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('محفظة', '${PaymentType.WALLET.name}')")
                    }
                }).build()
    }
}
