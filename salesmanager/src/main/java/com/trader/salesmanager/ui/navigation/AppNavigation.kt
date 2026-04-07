package com.trader.salesmanager.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.*
import androidx.navigation.compose.*
import com.trader.salesmanager.ui.activation.ActivationScreen
import com.trader.salesmanager.ui.activation.MerchantEvent
import com.trader.salesmanager.ui.activation.MerchantWatcherViewModel
import com.trader.salesmanager.ui.chat.ChatScreen
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val watcherVm: MerchantWatcherViewModel = koinViewModel()
    val expiryBanner by watcherVm.expiryBanner.collectAsState()
    var showExpiredDialog by remember { mutableStateOf<String?>(null) }

    // React to merchant events
    LaunchedEffect(Unit) {
        watcherVm.event.collect { event ->
            when (event) {
                is MerchantEvent.Disabled ->
                    showExpiredDialog = "تم تعطيل حسابك من قِبل الإدارة."
                is MerchantEvent.Deleted  ->
                    showExpiredDialog = "تم حذف حسابك. تواصل مع الإدارة."
                is MerchantEvent.Expired  ->
                    showExpiredDialog = "انتهت مدة اشتراكك. تواصل مع الإدارة للتجديد."
                is MerchantEvent.ExpiryWarning -> { /* notification already shown */ }
            }
        }
    }

    // Force logout dialog
    showExpiredDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.Warning, null, tint = Color(0xFFF59E0B)) },
            title = { Text("انتبه", fontWeight = FontWeight.Bold) },
            text  = { Text(msg) },
            confirmButton = {
                Button(onClick = {
                    showExpiredDialog = null
                    navController.navigate(Screen.Activation.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) { Text("حسناً") }
            }
        )
    }

    // Expiry warning banner (≤7 days)
    expiryBanner?.let { days ->
        if (days in 1..7) {
            // shown as notification — no in-app banner needed
        }
    }

    NavHost(
        navController   = navController,
        startDestination = Screen.Activation.route,
        enterTransition  = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
        exitTransition   = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
        popEnterTransition  = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition   = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(300)) }
    ) {
        composable(Screen.Activation.route) {
            ActivationScreen(onActivated = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Activation.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCustomers    = { navController.navigate(Screen.Customers.route) },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToReports      = { navController.navigate(Screen.Reports.route) },
                onNavigateToDebts        = { navController.navigate(Screen.Debts.route) },
                onNavigateToSettings     = { navController.navigate(Screen.Settings.route) },
                onNavigateToChat         = { navController.navigate(Screen.Chat.route) },
                onAddTransaction         = { navController.navigate(Screen.AddTransaction.route) }
            )
        }
        composable(Screen.Customers.route) {
            CustomersScreen(
                onNavigateUp   = { navController.navigateUp() },
                onCustomerClick = { navController.navigate(Screen.CustomerDetails.route(it)) },
                onAddCustomer   = { navController.navigate(Screen.AddCustomer.route) }
            )
        }
        composable(Screen.AddCustomer.route) {
            AddEditCustomerScreen(customerId = null, onNavigateUp = { navController.navigateUp() })
        }
        composable(Screen.EditCustomer.route, listOf(navArgument("customerId") { type = NavType.LongType })) {
            AddEditCustomerScreen(
                customerId  = it.arguments!!.getLong("customerId"),
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.CustomerDetails.route, listOf(navArgument("customerId") { type = NavType.LongType })) {
            CustomerDetailsScreen(
                customerId       = it.arguments!!.getLong("customerId"),
                onNavigateUp     = { navController.navigateUp() },
                onEditCustomer   = { navController.navigate(Screen.EditCustomer.route(it.arguments!!.getLong("customerId"))) },
                onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                onTransactionClick = { id -> navController.navigate(Screen.TransactionDetails.route(id)) }
            )
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateUp       = { navController.navigateUp() },
                onTransactionClick = { navController.navigate(Screen.TransactionDetails.route(it)) },
                onAddTransaction   = { navController.navigate(Screen.AddTransaction.route) }
            )
        }
        composable(Screen.AddTransaction.route) {
            AddEditTransactionScreen(transactionId = null, onNavigateUp = { navController.navigateUp() })
        }
        composable(Screen.EditTransaction.route, listOf(navArgument("transactionId") { type = NavType.LongType })) {
            AddEditTransactionScreen(
                transactionId = it.arguments!!.getLong("transactionId"),
                onNavigateUp  = { navController.navigateUp() }
            )
        }
        composable(Screen.TransactionDetails.route, listOf(navArgument("transactionId") { type = NavType.LongType })) {
            TransactionDetailsScreen(
                transactionId = it.arguments!!.getLong("transactionId"),
                onNavigateUp  = { navController.navigateUp() },
                onEdit        = { id -> navController.navigate(Screen.EditTransaction.route(id)) }
            )
        }
        composable(Screen.Debts.route) {
            DebtsScreen(
                onNavigateUp    = { navController.navigateUp() },
                onCustomerClick = { navController.navigate(Screen.CustomerDetails.route(it)) }
            )
        }
        composable(Screen.Reports.route)  { ReportsScreen(onNavigateUp = { navController.navigateUp() }) }
        composable(Screen.Payments.route) { PaymentMethodsScreen(onNavigateUp = { navController.navigateUp() }) }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateUp              = { navController.navigateUp() },
                onNavigateToPaymentMethods = { navController.navigate(Screen.Payments.route) }
            )
        }
        composable(Screen.Chat.route) { ChatScreen(onNavigateUp = { navController.navigateUp() }) }
    }
}
