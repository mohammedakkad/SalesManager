package com.trader.core.data.repository

import com.trader.core.data.local.dao.EmployeeDao
import com.trader.core.data.local.entity.EmployeeEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Employee
import com.trader.core.domain.model.EmployeeRole
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.EmployeeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class EmployeeRepositoryImpl(
    private val dao: EmployeeDao,
    private val merchantId: String,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : EmployeeRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    override fun observeEmployees(): Flow<List<Employee>> =
        dao.getAllByMerchant(merchantId).map { list -> list.map { it.toDomain() } }

    override suspend fun addOrUpdate(employee: Employee) {
        val entity = EmployeeEntity.fromDomain(employee.copy(merchantId = merchantId))
        dao.insertOrUpdate(entity)

        syncScope.launch {
            runCatching {
                sync.pushEmployee(merchantId, employee)
                dao.markSynced(employee.id)
            }
        }
    }

    override suspend fun getById(id: String): Employee? =
        dao.getById(id)?.toDomain()

    override suspend fun deleteById(id: String) {
        dao.deleteById(id)

        syncScope.launch {
            runCatching { sync.deleteEmployee(merchantId, id) }
        }
    }

    override suspend fun getByPin(pin: String): Employee? =
        dao.getByPin(merchantId, pin)?.toDomain()

    override suspend fun ensureDefaultAdmin() {
        val count = dao.countByMerchant(merchantId)
        if (count > 0) return

        val defaultAdmin = Employee(
            id = "admin_$merchantId",
            merchantId = merchantId,
            name = "المدير",
            pinCode = "0000",
            role = EmployeeRole.ADMIN,
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        dao.insertOrUpdate(EmployeeEntity.fromDomain(defaultAdmin))

        syncScope.launch {
            runCatching {
                sync.pushEmployee(merchantId, defaultAdmin)
                dao.markSynced(defaultAdmin.id)
            }
        }
    }

    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { code ->
                    sync.observeEmployees(code).collect { remoteEmployees ->
                        val remoteIds = remoteEmployees.map { it.id }.toSet()

                        remoteEmployees.forEach { employee ->
                            runCatching {
                                dao.insertOrUpdate(
                                    EmployeeEntity.fromDomain(
                                        employee.copy(
                                            merchantId = code,
                                            syncStatus = SyncStatus.SYNCED
                                        )
                                    )
                                )
                            }
                        }

                        val localIds = dao.getAllIds(code)
                        localIds.forEach { localId ->
                            if (localId !in remoteIds) {
                                dao.deleteById(localId)
                            }
                        }
                    }
                }
        }
    }
}
