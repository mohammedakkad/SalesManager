package com.trader.salesmanager.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trader.salesmanager.ui.activation.ActivationScreen
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerScreen
import com.trader.salesmanager.ui.customers.details.CustomerDetailsScreen
import com.trader.salesmanager.ui.customers.list.CustomersScreen
import com.trader.salesmanager.ui.debts.DebtsScreen
import com.trader.salesmanager.ui.home.HomeScreen
import com.trader.salesmanager.ui.payments.PaymentMethodsScreen
import com.trader.salesmanager.ui.reports.ReportsScreen
import com.trader.salesmanager.ui.settings.SettingsScreen
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionScreen
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsScreen
import com.trader.salesmanager.ui.transactions.list.TransactionsScreen
import org.koin.androidx.compose.koinViewModel
import com.trader.salesmanager.ui.activation.ActivationViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val activationVm: ActivationViewModel = koinViewModel()
    val isActivated by activationVm.isActivated.collectAsState()

    val startDestination = if (isActivated == true) Screen.Home.route else Screen.Activation.route

    if (isActivated == null) return // Loading state

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Activation.route) {
            ActivationScreen(onActivated = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Activation.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCustomers      = { navController.navigate(Screen.CustomersList.route) },
                onNavigateToTransactions   = { navController.navigate(Screen.TransactionsList.route) },
                onNavigateToReports        = { navController.navigate(Screen.Reports.route) },
                onNavigateToDebts          = { navController.navigate(Screen.Debts.route) },
                onNavigateToSettings       = { navController.navigate(Screen.Settings.route) },
                onAddTransaction           = { navController.navigate(Screen.AddTransaction.createRoute()) }
            )
        }
        composable(Screen.CustomersList.route) {
            CustomersScreen(
                onNavigateUp      = { navController.navigateUp() },
                onCustomerClick   = { navController.navigate(Screen.CustomerDetails.createRoute(it)) },
                onAddCustomer     = { navController.navigate(Screen.AddCustomer.route) }
            )
        }
        composable(Screen.AddCustomer.route) {
            AddEditCustomerScreen(
                customerId  = null,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.EditCustomer.route, arguments = listOf(navArgument("customerId") { type = NavType.LongType })) {
            AddEditCustomerScreen(
                customerId  = it.arguments?.getLong("customerId"),
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.CustomerDetails.route, arguments = listOf(navArgument("customerId") { type = NavType.LongType })) {
            val customerId = it.arguments!!.getLong("customerId")
            CustomerDetailsScreen(
                customerId          = customerId,
                onNavigateUp        = { navController.navigateUp() },
                onEditCustomer      = { navController.navigate(Screen.EditCustomer.createRoute(customerId)) },
                onAddTransaction    = { navController.navigate(Screen.AddTransaction.createRoute(customerId)) },
                onTransactionClick  = { tid -> navController.navigate(Screen.TransactionDetails.createRoute(tid)) }
            )
        }
        composable(Screen.TransactionsList.route) {
            TransactionsScreen(
                onNavigateUp      = { navController.navigateUp() },
                onTransactionClick = { navController.navigate(Screen.TransactionDetails.createRoute(it)) },
                onAddTransaction  = { navController.navigate(Screen.AddTransaction.createRoute()) }
            )
        }
        composable(
            Screen.AddTransaction.route,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            val customerId = it.arguments?.getLong("customerId")?.takeIf { id -> id != -1L }
            AddEditTransactionScreen(
                transactionId = null,
                preselectedCustomerId = customerId,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.EditTransaction.route, arguments = listOf(navArgument("transactionId") { type = NavType.LongType })) {
            AddEditTransactionScreen(
                transactionId = it.arguments?.getLong("transactionId"),
                preselectedCustomerId = null,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.TransactionDetails.route, arguments = listOf(navArgument("transactionId") { type = NavType.LongType })) {
            TransactionDetailsScreen(
                transactionId = it.arguments!!.getLong("transactionId"),
                onNavigateUp  = { navController.navigateUp() },
                onEdit        = { tid -> navController.navigate(Screen.EditTransaction.createRoute(tid)) }
            )
        }
        composable(Screen.Reports.route) { ReportsScreen(onNavigateUp = { navController.navigateUp() }) }
        composable(Screen.PaymentMethods.route) { PaymentMethodsScreen(onNavigateUp = { navController.navigateUp() }) }
        composable(Screen.Debts.route) { DebtsScreen(onNavigateUp = { navController.navigateUp() }, onCustomerClick = { navController.navigate(Screen.CustomerDetails.createRoute(it)) }) }
        composable(Screen.Settings.route) { SettingsScreen(onNavigateUp = { navController.navigateUp() }, onNavigateToPaymentMethods = { navController.navigate(Screen.PaymentMethods.route) }) }
    }
}