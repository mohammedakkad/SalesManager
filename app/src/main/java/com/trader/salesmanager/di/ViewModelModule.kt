package com.trader.salesmanager.di

import com.trader.salesmanager.ui.activation.ActivationViewModel
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerViewModel
import com.trader.salesmanager.ui.customers.details.CustomerDetailsViewModel
import com.trader.salesmanager.ui.customers.list.CustomersViewModel
import com.trader.salesmanager.ui.debts.DebtsViewModel
import com.trader.salesmanager.ui.home.HomeViewModel
import com.trader.salesmanager.ui.payments.PaymentMethodsViewModel
import com.trader.salesmanager.ui.reports.ReportsViewModel
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import com.trader.salesmanager.ui.transactions.list.TransactionsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { ActivationViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { CustomersViewModel(get()) }
    viewModel { AddEditCustomerViewModel(get()) }
    viewModel { parameters -> CustomerDetailsViewModel(get(), get(), parameters.get()) }
    viewModel { TransactionsViewModel(get()) }
    viewModel { AddEditTransactionViewModel(get(), get()) }
    viewModel { ReportsViewModel(get()) }
    viewModel { PaymentMethodsViewModel(get()) }
    viewModel { DebtsViewModel(get(), get()) }
}