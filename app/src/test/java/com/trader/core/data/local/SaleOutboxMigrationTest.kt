package com.trader.core.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.domain.model.TransactionReturnStatus
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Version24 is the authentic pre-command Room export. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class SaleOutboxMigrationTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private val name="migration-sale-outbox-test.db"
    private var room:AppDatabase?=null
    private val schema by lazy { JsonParser.parseString(context.assets.open("com.trader.core.data.local.db.AppDatabase/24.json").bufferedReader().use { it.readText() }).asJsonObject["database"].asJsonObject }
    private fun legacy():SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object:SupportSQLiteOpenHelper.Callback(24) {
            override fun onCreate(db:SupportSQLiteDatabase) {
                schema["entities"].asJsonArray.forEach { entry ->
                    val e=entry.asJsonObject
                    db.execSQL(e["createSql"].asString.replace("\${TABLE_NAME}",e["tableName"].asString))
                    e["indices"].asJsonArray.forEach { db.execSQL(it.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}",e["tableName"].asString)) }
                }
                schema["setupQueries"].asJsonArray.forEach { db.execSQL(it.asString) }
            }
            override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int) { error("Unexpected legacy upgrade") }
        }).build())
    private fun insert(db:SupportSQLiteDatabase,table:String,values:Map<String,Any?>) {
        val entity=schema["entities"].asJsonArray.first { it.asJsonObject["tableName"].asString==table }.asJsonObject
        val c=ContentValues()
        entity["fields"].asJsonArray.forEach {
            val f=it.asJsonObject; val key=f["columnName"].asString
            val value=if(values.containsKey(key)) values[key] else if(f["notNull"].asBoolean) {
                if(f["affinity"].asString=="TEXT") "" else 0
            } else null
            when(value) {
                null -> c.putNull(key)
                is String -> c.put(key,value)
                is Double -> c.put(key,value)
                is Int -> c.put(key,value)
                is Long -> c.put(key,value)
                else -> error("Unsupported fixture type")
            }
        }
        db.insert(table,0,c)
    }
    @Before fun seed() {
        legacy().use { helper ->
            val db=helper.writableDatabase
            insert(db,"customers",mapOf("id" to 7,"name" to "fixture"))
            for((id,amount) in listOf(1 to 75.0,2 to 0.0,3 to 20.0,4 to 0.0,5 to 15.0)) {
                insert(db,"transactions",mapOf("id" to id,"customerId" to 7,"amount" to amount,"syncStatus" to "PENDING","paymentType" to "DEBT"))
            }
            for(id in listOf(1,2,4)) {
                insert(db,"invoice_items",mapOf("id" to "line$id","transactionId" to id,"unitId" to "unit","quantity" to (if(id==4) 2.0 else 4.0),"pricePerUnit" to 25.0,"syncStatus" to "PENDING"))
                insert(db,"return_invoices",mapOf("id" to "return$id","originalTransactionId" to id,"totalRefund" to (if(id==1) 25.0 else 100.0),"returnType" to "PARTIAL","syncStatus" to "PENDING"))
                insert(db,"return_items",mapOf("id" to "returned$id","returnInvoiceId" to "return$id","unitId" to "unit","returnedQuantity" to (if(id==1) 1.0 else 4.0)))
            }
            // Two invoice lines for the same unit: compare total sold, not each line separately.
            insert(db,"invoice_items",mapOf("id" to "line4b","transactionId" to 4,"unitId" to "unit","quantity" to 2.0,"syncStatus" to "PENDING"))
            insert(db,"return_invoices",mapOf("id" to "return5","originalTransactionId" to 5,"totalRefund" to 5.0,"returnType" to "PARTIAL","syncStatus" to "FAILED"))
            insert(db,"cash_boxes",mapOf("id" to "box","paymentMethodId" to 10,"currentBalance" to 42.5,"syncStatus" to "PENDING"))
            insert(db,"stock_movements",mapOf("id" to "stock","quantity" to 1.0,"quantityBefore" to 3.0,"quantityAfter" to 4.0,"movementType" to "RETURN_IN","syncStatus" to "PENDING"))
        }
    }
    private fun upgraded(migration:Migration=AppDatabase.MIGRATION_24_25):AppDatabase = Room.databaseBuilder(context,AppDatabase::class.java,name)
        .allowMainThreadQueries().addMigrations(migration).build().also { room=it }
    @After fun cleanup() { room?.close();context.deleteDatabase(name) }
    @Test fun migrationPreservesAllFinancialRowsAndValidatesRoomSchema()=runTest {
        val before=legacy().use { snapshot(it.writableDatabase) }
        val upgraded=upgraded()
        assertEquals(before,snapshot(upgraded.openHelper.writableDatabase))
        assertTrue(upgraded.saleOutboxDao().all().isEmpty())
        upgraded.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }
    private fun snapshot(db:SupportSQLiteDatabase)=listOf("transactions","invoice_items","return_invoices","return_items","cash_boxes","stock_movements").associateWith { table ->
        db.query("SELECT * FROM $table ORDER BY rowid").use { c ->
            buildList { while(c.moveToNext()) add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) }) }
        }
    }
    @Test fun injectedMigrationFailureRollsBackSchemaAndData() {
        val before=legacy().use { snapshot(it.writableDatabase) }
        val failing=object:Migration(24,25) {
            override fun migrate(db:SupportSQLiteDatabase) { AppDatabase.MIGRATION_24_25.migrate(db);error("injected failure") }
        }
        assertThrows(IllegalStateException::class.java) { upgraded(failing).openHelper.writableDatabase }
        room?.close();room=null
        legacy().use { helper ->
            val db=helper.writableDatabase
            assertEquals(24,db.version)
            assertEquals(before,snapshot(db))
            db.query("SELECT name FROM sqlite_master WHERE name='sale_outbox'").use { assertFalse(it.moveToFirst()) }
        }
    }
}
