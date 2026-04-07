package com.trader.core.data.local.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.PaymentType

@Database(
    entities = [CustomerEntity::class, TransactionEntity::class, PaymentMethodEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentMethodDao(): PaymentMethodDao

    companion object {
        const val DB_NAME = "sales_manager.db"

        // Migration v1→v2: add phone column to customers
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context) = Room.databaseBuilder(
            context, AppDatabase::class.java, DB_NAME
        )
        .addMigrations(MIGRATION_1_2)
        .addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('كاش', '${PaymentType.CASH.name}')")
                db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('بنك', '${PaymentType.BANK.name}')")
                db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('محفظة', '${PaymentType.WALLET.name}')")
            }
        }).build()
    }
}