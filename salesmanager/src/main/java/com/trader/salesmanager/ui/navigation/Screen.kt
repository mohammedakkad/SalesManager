package com.trader.salesmanager.ui.navigation

sealed class Screen(val route: String) {
    object Activation       : Screen("activation")
    object Home             : Screen("home")
    object Customers        : Screen("customers")
    object AddCustomer      : Screen("customers/add")
    object EditCustomer     : Screen("customers/{customerId}/edit") {
        fun route(id: Long) = "customers/$id/edit"
    }
    object CustomerDetails  : Screen("customers/{customerId}") {
        fun route(id: Long) = "customers/$id"
    }
    object Transactions     : Screen("transactions")
    object AddTransaction   : Screen("transactions/add")
    object EditTransaction  : Screen("transactions/{transactionId}/edit") {
        fun route(id: Long) = "transactions/$id/edit"
    }
    object TransactionDetails : Screen("transactions/{transactionId}") {
        fun route(id: Long) = "transactions/$id"
    }
    object Debts            : Screen("debts")
    object Reports          : Screen("reports")
    object Payments         : Screen("payments")
    object Settings         : Screen("settings")
    object Chat             : Screen("chat")
}