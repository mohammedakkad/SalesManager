package com.trader.salesmanager.ui.settings.backup

import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CashBoxEntity
import com.trader.core.data.local.entity.CashBoxMovementEntity
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.EmployeeEntity
import com.trader.core.data.local.entity.InventorySessionEntity
import com.trader.core.data.local.entity.InventorySessionItemEntity
import com.trader.core.data.local.entity.InvoiceItemEntity
import com.trader.core.data.local.entity.PaymentMethodEntity
import com.trader.core.data.local.entity.ProductEntity
import com.trader.core.data.local.entity.ProductUnitEntity
import com.trader.core.data.local.entity.ReturnInvoiceEntity
import com.trader.core.data.local.entity.ReturnItemEntity
import com.trader.core.data.local.entity.StockMovementEntity
import com.trader.core.data.local.entity.TransactionEntity

const val BACKUP_SCHEMA_VERSION = 18
const val BACKUP_JSON_ENTRY = "backup.json"

data class BackupPayload(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val customers: List<CustomerEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val productUnits: List<ProductUnitEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val invoiceItems: List<InvoiceItemEntity> = emptyList(),
    val inventorySessions: List<InventorySessionEntity> = emptyList(),
    val inventorySessionItems: List<InventorySessionItemEntity> = emptyList(),
    val returnInvoices: List<ReturnInvoiceEntity> = emptyList(),
    val returnItems: List<ReturnItemEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val cashBoxes: List<CashBoxEntity> = emptyList(),
    val cashBoxMovements: List<CashBoxMovementEntity> = emptyList()
) {
    fun isCompatible(): Boolean = schemaVersion == BACKUP_SCHEMA_VERSION
}
