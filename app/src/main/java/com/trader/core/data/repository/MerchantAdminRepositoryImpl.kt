package com.trader.core.data.repository

import com.trader.core.data.remote.MerchantAdminService
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.Flow

class MerchantAdminRepositoryImpl(private val service: MerchantAdminService) :
    MerchantAdminRepository {
    override fun getAllMerchants(): Flow<List<Merchant>> = service.getAllMerchants()

    override suspend fun getMerchantById(id: String): Merchant? = service.getMerchantById(id)

    override suspend fun addMerchant(merchant: Merchant): String = service.addMerchant(merchant)

    override suspend fun updateMerchant(merchant: Merchant) = service.updateMerchant(merchant)

    override suspend fun deleteMerchant(id: String) = service.deleteMerchant(id)

    override suspend fun setMerchantStatus(id: String, status: MerchantStatus) =
        service.setStatus(id, status)

    override suspend fun adjustExpiry(id: String, deltaDays: Int) =
        service.adjustExpiry(id, deltaDays)
}
