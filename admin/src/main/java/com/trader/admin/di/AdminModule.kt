package com.trader.admin.di

import com.trader.core.data.remote.ChatService
import com.trader.core.data.remote.MerchantAdminService
import com.trader.core.data.repository.ChatRepositoryImpl
import com.trader.core.data.repository.MerchantAdminRepositoryImpl
import com.trader.core.domain.repository.ChatRepository
import com.trader.core.domain.repository.MerchantAdminRepository
import com.trader.admin.ui.auth.AuthViewModel
import com.trader.admin.ui.dashboard.DashboardViewModel
import com.trader.admin.ui.merchants.list.MerchantsViewModel
import com.trader.admin.ui.merchants.add.AddMerchantViewModel
import com.trader.admin.ui.merchants.detail.MerchantDetailViewModel
import com.trader.admin.ui.chat.list.ChatListViewModel
import com.trader.admin.ui.chat.detail.ChatDetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val adminModule = module {
    // Services
    single { MerchantAdminService() }
    single { ChatService() }

    // Repositories
    single<MerchantAdminRepository> { MerchantAdminRepositoryImpl(get()) }
    single<ChatRepository>          { ChatRepositoryImpl(get()) }

    // ViewModels
    viewModel { AuthViewModel() }
    viewModel { DashboardViewModel(get()) }
    viewModel { MerchantsViewModel(get()) }
    viewModel { AddMerchantViewModel(get()) }
    viewModel { params -> MerchantDetailViewModel(get(), params.get()) }
    viewModel { ChatListViewModel(get(), get()) }
    viewModel { params -> ChatDetailViewModel(get(), params.get()) }
}
