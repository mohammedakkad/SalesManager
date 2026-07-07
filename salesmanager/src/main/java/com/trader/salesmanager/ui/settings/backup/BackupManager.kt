package com.trader.salesmanager.ui.settings.backup

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.trader.core.data.local.db.AppDatabase
import androidx.room.withTransaction
import com.trader.core.data.local.entity.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val merchantId: String
) {
    private val gson = Gson()

    suspend fun exportToZip(): File = withContext(Dispatchers.IO) {
        val payload = collectPayload()
        val json = gson.toJson(payload)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(context.cacheDir, "sales_backup_$timestamp.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        zipFile
    }

    suspend fun parseBackup(input: InputStream): BackupPayload = withContext(Dispatchers.IO) {
        val bytes = input.use { it.readBytes() }
        val json = extractJson(bytes) ?: throw BackupException.Corrupted
        try {
            gson.fromJson(json, BackupPayload::class.java)
                ?: throw BackupException.Corrupted
        } catch (_: JsonSyntaxException) {
            throw BackupException.Corrupted
        }
    }

    fun validatePayload(payload: BackupPayload) {
        if (!payload.isCompatible()) {
            throw BackupException.IncompatibleSchema(payload.schemaVersion)
        }
        if (payload.customers.isEmpty() && payload.transactions.isEmpty() &&
            payload.products.isEmpty() && payload.paymentMethods.isEmpty()
        ) {
            throw BackupException.EmptyBackup
        }
    }

    suspend fun restorePayload(payload: BackupPayload) = withContext(Dispatchers.IO) {
        validatePayload(payload)
        database.withTransaction {
            val customerDao = database.customerDao()
            val transactionDao = database.transactionDao()
            val paymentMethodDao = database.paymentMethodDao()
            val productDao = database.productDao()
            val stockMovementDao = database.stockMovementDao()
            val invoiceItemDao = database.invoiceItemDao()
            val inventoryDao = database.inventoryDao()
            val returnDao = database.returnDao()
            val employeeDao = database.employeeDao()
            val cashBoxDao = database.cashBoxDao()
            val cashBoxMovementDao = database.cashBoxMovementDao()

            cashBoxMovementDao.deleteAll()
            cashBoxDao.deleteAll()
            returnDao.deleteAllInvoices()
            invoiceItemDao.deleteAll()
            stockMovementDao.deleteAll()
            inventoryDao.deleteAllSessionItems()
            inventoryDao.deleteAllSessions()
            productDao.deleteAllProducts()
            transactionDao.deleteAll()
            customerDao.deleteAll()
            employeeDao.deleteAllByMerchant(merchantId)
            paymentMethodDao.deleteAll()

            if (payload.customers.isNotEmpty()) {
                customerDao.insertAll(payload.customers)
            } else {
                customerDao.insertAll(
                    listOf(
                        CustomerEntity(
                            id = -1,
                            name = "زبون زائر",
                            phone = "",
                            createdAt = 0,
                            syncStatus = "SYNCED"
                        )
                    )
                )
            }
            if (payload.paymentMethods.isNotEmpty()) paymentMethodDao.insertAll(payload.paymentMethods)
            if (payload.products.isNotEmpty()) productDao.insertProducts(payload.products)
            if (payload.productUnits.isNotEmpty()) productDao.insertUnits(payload.productUnits)
            if (payload.transactions.isNotEmpty()) transactionDao.insertAll(payload.transactions)
            if (payload.invoiceItems.isNotEmpty()) invoiceItemDao.insertAll(payload.invoiceItems)
            if (payload.stockMovements.isNotEmpty()) stockMovementDao.insertAll(payload.stockMovements)
            if (payload.inventorySessions.isNotEmpty()) inventoryDao.insertAllSessions(payload.inventorySessions)
            if (payload.inventorySessionItems.isNotEmpty()) {
                inventoryDao.insertSessionItems(payload.inventorySessionItems)
            }
            if (payload.returnInvoices.isNotEmpty()) returnDao.insertAllInvoices(payload.returnInvoices)
            if (payload.returnItems.isNotEmpty()) returnDao.insertReturnItems(payload.returnItems)
            if (payload.employees.isNotEmpty()) employeeDao.insertAll(payload.employees)
            if (payload.cashBoxes.isNotEmpty()) cashBoxDao.upsertAll(payload.cashBoxes)
            if (payload.cashBoxMovements.isNotEmpty()) cashBoxMovementDao.insertAll(payload.cashBoxMovements)

            transactionDao.recalculateHasItems()
        }
    }

    private suspend fun collectPayload(): BackupPayload {
        val productRelations = database.productDao().getAllWithUnitsOnce()
        return BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            customers = database.customerDao().getAllOnce(),
            transactions = database.transactionDao().getAllOnce(),
            paymentMethods = database.paymentMethodDao().getAllOnce(),
            products = productRelations.map { it.product },
            productUnits = productRelations.flatMap { it.units },
            stockMovements = database.stockMovementDao().getAllOnce(),
            invoiceItems = database.invoiceItemDao().getAllOnce(),
            inventorySessions = database.inventoryDao().getAllSessionsOnce(),
            inventorySessionItems = database.inventoryDao().getAllSessionItemsOnce(),
            returnInvoices = database.returnDao().getAllInvoicesOnce(),
            returnItems = database.returnDao().getAllItemsOnce(),
            employees = database.employeeDao().getAllOnce(merchantId),
            cashBoxes = database.cashBoxDao().getAllOnce(),
            cashBoxMovements = database.cashBoxMovementDao().getAllOnce()
        )
    }

    private fun extractJson(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            ZipInputStream(BufferedInputStream(bytes.inputStream())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".json")) {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
                null
            }
        } else {
            bytes.toString(Charsets.UTF_8)
        }
    }
}

sealed class BackupException : Exception() {
    data class IncompatibleSchema(val found: Int) : BackupException()
    data object Corrupted : BackupException()
    data object EmptyBackup : BackupException()
    data object StorageFailed : BackupException()
    data object ReadFailed : BackupException()
}
