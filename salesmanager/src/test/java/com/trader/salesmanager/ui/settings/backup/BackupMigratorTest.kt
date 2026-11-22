package com.trader.salesmanager.ui.settings.backup
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.TransactionEntity
import org.junit.Assert.*
import org.junit.Test
class BackupMigratorTest {
    // These are payload compatibility checks, not historical Room schema tests.
    @Test fun allSupportedPayloadVersionsPreserveFinancialRecords() {
        val customer=CustomerEntity(id=7,name="عميل",phone="123",createdAt=10)
        val sale=TransactionEntity(id=8,customerId=7,amount=123.45,isPaid=false,date=20)
        for(version in 1..BACKUP_FORMAT_VERSION) {
            val migrated=BackupMigrator.migrateToCurrent(BackupPayload(schemaVersion=version,exportedAt=30,customers=listOf(customer),transactions=listOf(sale)))
            assertEquals("version $version",BACKUP_FORMAT_VERSION,migrated.schemaVersion)
            assertEquals(listOf(customer),migrated.customers)
            assertEquals(listOf(sale),migrated.transactions)
            assertEquals(30L,migrated.exportedAt)
        }
    }
    @Test fun futurePayloadIsRejectedRatherThanRestored() { assertThrows(BackupException.NewerBackupVersion::class.java) { BackupMigrator.migrateToCurrent(BackupPayload(schemaVersion=BACKUP_FORMAT_VERSION+1)) } }
    @Test fun unsupportedOldPayloadIsRejectedRatherThanRestored() { assertThrows(BackupException.UnsupportedBackupVersion::class.java) { BackupMigrator.migrateToCurrent(BackupPayload(schemaVersion=0)) } }
}
