package com.trader.salesmanager.ui.settings.backup

import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.ProductUnitEntity
import com.trader.core.data.local.entity.TransactionEntity

object BackupMigrator {
    const val MIN_SUPPORTED_VERSION = 1

    fun migrateToCurrent(payload: BackupPayload): BackupPayload {
        if (payload.schemaVersion > BACKUP_FORMAT_VERSION) {
            throw BackupException.NewerBackupVersion(payload.schemaVersion)
        }
        if (payload.schemaVersion < MIN_SUPPORTED_VERSION) {
            throw BackupException.UnsupportedBackupVersion(payload.schemaVersion)
        }
        var current = normalize(payload)
        while (current.schemaVersion < BACKUP_FORMAT_VERSION) {
            current = migrateOneStep(current)
        }
        return current
    }

    private fun migrateOneStep(payload: BackupPayload): BackupPayload = when (payload.schemaVersion) {
        1 -> migrateV1ToV2(payload)
        2 -> migrateV2ToV3(payload)
        3 -> migrateV3ToV4(payload)
        4 -> migrateV4ToV5(payload)
        5 -> migrateV5ToV6(payload)
        6 -> migrateV6ToV7(payload)
        7 -> migrateV7ToV8(payload)
        8 -> migrateV8ToV9(payload)
        9 -> migrateV9ToV10(payload)
        10 -> migrateV10ToV11(payload)
        11 -> migrateV11ToV12(payload)
        12 -> migrateV12ToV13(payload)
        13 -> migrateV13ToV14(payload)
        14 -> migrateV14ToV15(payload)
        15 -> migrateV15ToV16(payload)
        16 -> migrateV16ToV17(payload)
        17 -> migrateV17ToV18(payload)
        else -> throw BackupException.UnsupportedBackupVersion(payload.schemaVersion)
    }

    private fun bump(payload: BackupPayload, toVersion: Int): BackupPayload =
        normalize(payload).copy(schemaVersion = toVersion)

    private fun migrateV1ToV2(payload: BackupPayload): BackupPayload = bump(
        payload.copy(
            customers = payload.customers.orEmpty().map { customer ->
                customer.copy(phone = customer.phone.ifBlank { "" })
            }
        ),
        2
    )

    private fun migrateV2ToV3(payload: BackupPayload): BackupPayload = bump(payload, 3)

    private fun migrateV3ToV4(payload: BackupPayload): BackupPayload = bump(
        payload.copy(
            products = payload.products.orEmpty(),
            productUnits = payload.productUnits.orEmpty(),
            stockMovements = payload.stockMovements.orEmpty(),
            invoiceItems = payload.invoiceItems.orEmpty(),
            inventorySessions = payload.inventorySessions.orEmpty(),
            inventorySessionItems = payload.inventorySessionItems.orEmpty()
        ),
        4
    )

    private fun migrateV4ToV5(payload: BackupPayload): BackupPayload = bump(payload, 5)
    private fun migrateV5ToV6(payload: BackupPayload): BackupPayload = bump(payload, 6)
    private fun migrateV6ToV7(payload: BackupPayload): BackupPayload = bump(payload, 7)
    private fun migrateV7ToV8(payload: BackupPayload): BackupPayload = bump(payload, 8)

    private fun migrateV8ToV9(payload: BackupPayload): BackupPayload = bump(
        payload.copy(
            transactions = payload.transactions.orEmpty().map { tx ->
                if (tx.syncStatus.isBlank()) tx.copy(syncStatus = "SYNCED") else tx
            }
        ),
        9
    )

    private fun migrateV9ToV10(payload: BackupPayload): BackupPayload = bump(payload, 10)

    private fun migrateV10ToV11(payload: BackupPayload): BackupPayload = bump(
        payload.copy(
            customers = payload.customers.orEmpty().map { customer ->
                if (customer.syncStatus.isBlank()) customer.copy(syncStatus = "SYNCED") else customer
            }
        ),
        11
    )

    private fun migrateV11ToV12(payload: BackupPayload): BackupPayload = bump(
        payload.copy(
            productUnits = payload.productUnits.orEmpty().map { unit ->
                unit.copy(costPrice = unit.costPrice.coerceAtLeast(0.0))
            }
        ),
        12
    )

    private fun migrateV12ToV13(payload: BackupPayload): BackupPayload = bump(
        payload.copy(
            returnInvoices = payload.returnInvoices.orEmpty(),
            returnItems = payload.returnItems.orEmpty()
        ),
        13
    )

    private fun migrateV13ToV14(payload: BackupPayload): BackupPayload = bump(payload, 14)

    private fun migrateV14ToV15(payload: BackupPayload): BackupPayload = bump(
        payload.copy(employees = payload.employees.orEmpty()),
        15
    )

    private fun migrateV15ToV16(payload: BackupPayload): BackupPayload = bump(payload, 16)

    private fun migrateV16ToV17(payload: BackupPayload): BackupPayload = bump(
        payload.copy(cashBoxes = payload.cashBoxes.orEmpty()),
        17
    )

    private fun migrateV17ToV18(payload: BackupPayload): BackupPayload = bump(
        payload.copy(cashBoxMovements = payload.cashBoxMovements.orEmpty()),
        18
    )

    private fun normalize(payload: BackupPayload): BackupPayload = payload.copy(
        customers = payload.customers.orEmpty().map(::normalizeCustomer),
        transactions = payload.transactions.orEmpty().map(::normalizeTransaction),
        paymentMethods = payload.paymentMethods.orEmpty(),
        products = payload.products.orEmpty(),
        productUnits = payload.productUnits.orEmpty().map(::normalizeProductUnit),
        stockMovements = payload.stockMovements.orEmpty(),
        invoiceItems = payload.invoiceItems.orEmpty(),
        inventorySessions = payload.inventorySessions.orEmpty(),
        inventorySessionItems = payload.inventorySessionItems.orEmpty(),
        returnInvoices = payload.returnInvoices.orEmpty(),
        returnItems = payload.returnItems.orEmpty(),
        employees = payload.employees.orEmpty(),
        cashBoxes = payload.cashBoxes.orEmpty(),
        cashBoxMovements = payload.cashBoxMovements.orEmpty()
    )

    private fun normalizeCustomer(customer: CustomerEntity): CustomerEntity =
        customer.copy(phone = customer.phone.ifBlank { "" })

    private fun normalizeTransaction(transaction: TransactionEntity): TransactionEntity =
        transaction.copy(
            syncStatus = transaction.syncStatus.ifBlank { "SYNCED" },
            note = transaction.note.ifBlank { "" }
        )

    private fun normalizeProductUnit(unit: ProductUnitEntity): ProductUnitEntity =
        unit.copy(costPrice = unit.costPrice.coerceAtLeast(0.0))
}
