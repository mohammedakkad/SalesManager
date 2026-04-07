package com.trader.salesmanager.ui.navigation

sealed class Screen(val route: String) {
    object Activation       : Screen("activation")
    object Home             : Screen("home")
    object CustomersList    : Screen("customers_list")
    object AddCustomer      : Screen("add_customer")
    object EditCustomer     : Screen("edit_customer/{customerId}") {
        fun createRoute(id: Long) = "edit_customer/$id"
    }
    object CustomerDetails  : Screen("customer_details/{customerId}") {
        fun createRoute(id: Long) = "customer_details/$id"
    }
    object TransactionsList : Screen("transactions_list")
    object AddTransaction   : Screen("add_transaction?customerId={customerId}") {
        fun createRoute(customerId: Long? = null) =
            if (customerId != null) "add_transaction?customerId=$customerId" else "add_transaction?customerId=-1"
    }
    object EditTransaction  : Screen("edit_transaction/{transactionId}") {
        fun createRoute(id: Long) = "edit_transaction/$id"
    }
    object TransactionDetails : Screen("transaction_details/{transactionId}") {
        fun createRoute(id: Long) = "transaction_details/$id"
    }
    object Reports          : Screen("reports")
    object PaymentMethods   : Screen("payment_methods")
    object Debts            : Screen("debts")
    object Settings         : Screen("settings")
    object Chat             : Screen("chat")
}
