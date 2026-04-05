package com.trader.core.data.local.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.PaymentType

@Database(
    entities = [CustomerEntity::class, TransactionEntity::class, PaymentMethodEntity::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentMethodDao(): PaymentMethodDao

    companion object {
        const val DB_NAME = "sales_manager.db"
        fun build(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('كاش', '${PaymentType.CASH.name}')")
                    db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('بنك', '${PaymentType.BANK.name}')")
                    db.execSQL("INSERT INTO payment_methods (name, type) VALUES ('محفظة', '${PaymentType.WALLET.name}')")
                }
            }).build()
    }
}
