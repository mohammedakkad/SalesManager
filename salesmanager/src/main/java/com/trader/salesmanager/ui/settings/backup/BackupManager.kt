package com.trader.salesmanager.ui.settings.backup

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.trader.core.data.local.db.AppDatabase
import androidx.room.withTransaction
import com.trader.core.data.local.entity.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal const val BACKUP_LOG_TAG = "BackupRestore"

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val merchantId: String
) {
    private val gson = Gson()

    suspend fun exportEncryptedBackup(password: CharArray): File = withContext(Dispatchers.IO) {
        try {
            val payload = collectPayload()
            val zipBytes = compressJsonToZip(gson.toJson(payload))
            val encrypted = BackupCrypto.encryptZip(
                zipBytes = zipBytes,
                password = password,
                schemaVersion = payload.schemaVersion,
                exportedAt = payload.exportedAt
            )
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(context.cacheDir, "sales_backup_$timestamp.$BACKUP_FILE_EXTENSION")
            FileOutputStream(file).use { it.write(encrypted) }
            file
        } catch (e: Exception) {
            Log.e(BACKUP_LOG_TAG, "Encrypted export failed: ${e.javaClass.simpleName}", e)
            throw BackupException.StorageFailed
        }
    }

    suspend fun parseBackupFromBytes(bytes: ByteArray, password: CharArray? = null): BackupPayload =
        withContext(Dispatchers.IO) {
            if (bytes.isEmpty()) throw BackupException.ReadFailed
            val zipBytes = when {
                BackupCrypto.isEncryptedBackup(bytes) -> {
                    if (password == null) throw BackupException.PasswordRequired
                    BackupCrypto.decryptToZip(bytes, password)
                }
                looksLikeZip(bytes) -> bytes
                else -> throw BackupException.InvalidFile
            }
            val json = try {
                extractJsonFromZip(zipBytes)
            } catch (e: ZipException) {
                Log.e(BACKUP_LOG_TAG, "Zip extraction failed", e)
                throw BackupException.Corrupted
            } ?: throw BackupException.Corrupted
            val rawPayload = parseJson(json)
            BackupMigrator.migrateToCurrent(rawPayload)
        }

    fun requiresPassword(bytes: ByteArray): Boolean = BackupCrypto.isEncryptedBackup(bytes)

    fun validatePayload(payload: BackupPayload) {
        val hasData = payload.customers.orEmpty().isNotEmpty() ||
            payload.transactions.orEmpty().isNotEmpty() ||
            payload.products.orEmpty().isNotEmpty() ||
            payload.paymentMethods.orEmpty().isNotEmpty()
        if (!hasData) {
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

            if (payload.customers.orEmpty().isNotEmpty()) {
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
            payload.paymentMethods.orEmpty().takeIf { it.isNotEmpty() }?.let {
                paymentMethodDao.insertAll(it)
            }
            payload.products.orEmpty().takeIf { it.isNotEmpty() }?.let {
                productDao.insertProducts(it)
            }
            payload.productUnits.orEmpty().takeIf { it.isNotEmpty() }?.let {
                productDao.insertUnits(it)
            }
            payload.transactions.orEmpty().takeIf { it.isNotEmpty() }?.let {
                transactionDao.insertAll(it)
            }
            payload.invoiceItems.orEmpty().takeIf { it.isNotEmpty() }?.let {
                invoiceItemDao.insertAll(it)
            }
            payload.stockMovements.orEmpty().takeIf { it.isNotEmpty() }?.let {
                stockMovementDao.insertAll(it)
            }
            payload.inventorySessions.orEmpty().takeIf { it.isNotEmpty() }?.let {
                inventoryDao.insertAllSessions(it)
            }
            payload.inventorySessionItems.orEmpty().takeIf { it.isNotEmpty() }?.let {
                inventoryDao.insertSessionItems(it)
            }
            payload.returnInvoices.orEmpty().takeIf { it.isNotEmpty() }?.let {
                returnDao.insertAllInvoices(it)
            }
            payload.returnItems.orEmpty().takeIf { it.isNotEmpty() }?.let {
                returnDao.insertReturnItems(it)
            }
            payload.employees.orEmpty().takeIf { it.isNotEmpty() }?.let {
                employeeDao.insertAll(it)
            }
            payload.cashBoxes.orEmpty().takeIf { it.isNotEmpty() }?.let {
                cashBoxDao.upsertAll(it)
            }
            payload.cashBoxMovements.orEmpty().takeIf { it.isNotEmpty() }?.let {
                cashBoxMovementDao.insertAll(it)
            }

            transactionDao.recalculateHasItems()
        }
    }

    private fun parseJson(json: String): BackupPayload {
        return try {
            gson.fromJson(json, BackupPayload::class.java) ?: throw BackupException.Corrupted
        } catch (e: JsonSyntaxException) {
            Log.e(BACKUP_LOG_TAG, "JSON syntax error during backup parse", e)
            throw BackupException.Corrupted
        } catch (e: JsonIOException) {
            Log.e(BACKUP_LOG_TAG, "JSON IO error during backup parse", e)
            throw BackupException.Corrupted
        } catch (e: IllegalStateException) {
            Log.e(BACKUP_LOG_TAG, "JSON structure mismatch during backup parse", e)
            throw BackupException.Corrupted
        }
    }

    private suspend fun collectPayload(): BackupPayload {
        val productRelations = database.productDao().getAllWithUnitsOnce()
        return BackupPayload(
            schemaVersion = BACKUP_FORMAT_VERSION,
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

    private fun compressJsonToZip(json: String): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private fun extractJsonFromZip(bytes: ByteArray): String? {
        ZipInputStream(BufferedInputStream(bytes.inputStream())).use { zip ->
            var entry = zip.nextEntry
            var fallback: String? = null
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".json")) {
                    val content = zip.readBytes().toString(Charsets.UTF_8)
                    if (entry.name == BACKUP_JSON_ENTRY || entry.name.endsWith("/$BACKUP_JSON_ENTRY")) {
                        return content
                    }
                    if (fallback == null) fallback = content
                }
                entry = zip.nextEntry
            }
            return fallback
        }
    }
}

sealed class BackupException : Exception() {
    data class NewerBackupVersion(val found: Int) : BackupException()
    data class UnsupportedBackupVersion(val found: Int) : BackupException()
    data object Corrupted : BackupException()
    data object WrongPasswordOrCorrupted : BackupException()
    data object PasswordRequired : BackupException()
    data object EmptyBackup : BackupException()
    data object InvalidFile : BackupException()
    data object StorageFailed : BackupException()
    data object ReadFailed : BackupException()
}
