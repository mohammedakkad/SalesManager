package com.trader.salesmanager.ui.navigation

sealed class Screen(val route: String) {
    object Activation         : Screen("activation")
    object Home               : Screen("home")
    object CustomersList      : Screen("customers")
    object AddCustomer        : Screen("customers/add")
    object EditCustomer       : Screen("customers/edit/{customerId}") {
        fun createRoute(customerId: Long) = "customers/edit/$customerId"
    }
    object CustomerDetails    : Screen("customers/{customerId}") {
        fun createRoute(customerId: Long) = "customers/$customerId"
    }
    object TransactionsList   : Screen("transactions")
    object AddTransaction     : Screen("transactions/add?customerId={customerId}") {
        fun createRoute(customerId: Long? = null) =
            if (customerId != null) "transactions/add?customerId=$customerId" else "transactions/add?customerId=-1"
    }
    object EditTransaction    : Screen("transactions/edit/{transactionId}") {
        fun createRoute(transactionId: Long) = "transactions/edit/$transactionId"
    }
    object TransactionDetails : Screen("transactions/{transactionId}") {
        fun createRoute(transactionId: Long) = "transactions/$transactionId"
    }
    object Reports            : Screen("reports")
    object PaymentMethods     : Screen("payment-methods")
    object Debts              : Screen("debts")
    object Settings           : Screen("settings")
}