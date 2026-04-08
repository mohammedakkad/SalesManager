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
import com.trader.salesmanager.ui.activation.ActivationViewModel
import com.trader.salesmanager.ui.activation.StartupState
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
    val activationVm: ActivationViewModel = koinViewModel()
    val watcherVm: MerchantWatcherViewModel = koinViewModel()

    val startupState by activationVm.startupState.collectAsState()
    var liveBlockMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        watcherVm.event.collect {
            event ->
            val msg = when (event) {
                is MerchantEvent.Disabled -> "تم تعطيل حسابك من قِبل الإدارة."
                is MerchantEvent.Deleted -> "تم حذف حسابك. تواصل مع الإدارة."
                is MerchantEvent.Expired -> "انتهت مدة اشتراكك. تواصل مع الإدارة للتجديد."
                is MerchantEvent.ExpiryWarning -> null
            }
            if (msg != null) liveBlockMessage = msg
        }
    }

    liveBlockMessage?.let {
        msg ->
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFF59E0B))
            },
            title = {
                Text("تنبيه", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(msg)
            },
            confirmButton = {
                Button(onClick = {
                    liveBlockMessage = null
                    activationVm.deactivate()
                    navController.navigate(Screen.Activation.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }) {
                    Text("حسناً")
                }
            }
        )
    }

    when (val s = startupState) {
        is StartupState.Checking -> {
            SplashCheckScreen()
            return
        }
        is StartupState.Blocked -> {
            BlockedScreen(
                message = s.message,
                canRetry = s.canRetry,
                onRetry = {
                    activationVm.checkStartup()
                }
            )
            return
        } else -> {
            /* ACTIVE or NeedActivation — continue to NavHost */
        }
    }

    val start = if (startupState == StartupState.Proceed)
        Screen.Home.route else Screen.Activation.route

    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = {
            slideInHorizontally(tween(280)) {
                it / 4
            } + fadeIn(tween(280))
        },
        exitTransition = {
            slideOutHorizontally(tween(280)) {
                -it / 4
            } + fadeOut(tween(280))
        },
        popEnterTransition = {
            slideInHorizontally(tween(280)) {
                -it / 4
            } + fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutHorizontally(tween(280)) {
                it / 4
            } + fadeOut(tween(280))
        }
    ) {
        composable(Screen.Activation.route) {
            ActivationScreen(onActivated = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Activation.route) {
                        inclusive = true
                    }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCustomers = {
                    navController.navigate(Screen.CustomersList.route)
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.TransactionsList.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToDebts = {
                    navController.navigate(Screen.Debts.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                }
            )
        }
        composable(Screen.CustomersList.route) {
            CustomersScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onCustomerClick = {
                    navController.navigate(Screen.CustomerDetails.createRoute(it))
                },
                onAddCustomer = {
                    navController.navigate(Screen.AddCustomer.route)
                }
            )
        }
        composable(Screen.AddCustomer.route) {
            AddEditCustomerScreen(customerId = null, onNavigateUp = {
                navController.navigateUp()
            })
        }
        composable(
            Screen.EditCustomer.route,
            listOf(navArgument("customerId") {
                type = NavType.LongType
            })
        ) {
            AddEditCustomerScreen(
                customerId = it.arguments!!.getLong("customerId"),
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            Screen.CustomerDetails.route,
            listOf(navArgument("customerId") {
                type = NavType.LongType
            })
        ) {
            back ->
            val id = back.arguments!!.getLong("customerId")
            CustomerDetailsScreen(
                customerId = id,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onEditCustomer = {
                    navController.navigate(Screen.EditCustomer.createRoute(id))
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute(id))
                },
                onTransactionClick = {
                    navController.navigate(Screen.TransactionDetails.createRoute(it))
                }
            )
        }
        composable(Screen.TransactionsList.route) {
            TransactionsScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onTransactionClick = {
                    navController.navigate(Screen.TransactionDetails.createRoute(it))
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                }
            )
        }
        composable(
            Screen.AddTransaction.route,
            listOf(navArgument("customerId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            back ->
            val preselectedId = back.arguments!!.getLong("customerId").takeIf {
                it != -1L
            }
            AddEditTransactionScreen(
                transactionId = null,
                preselectedCustomerId = preselectedId,
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            Screen.EditTransaction.route,
            listOf(navArgument("transactionId") {
                type = NavType.LongType
            })
        ) {
            AddEditTransactionScreen(
                transactionId = it.arguments!!.getLong("transactionId"),
                preselectedCustomerId = null,
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            Screen.TransactionDetails.route,
            listOf(navArgument("transactionId") {
                type = NavType.LongType
            })
        ) {
            back ->
            val id = back.arguments!!.getLong("transactionId")
            TransactionDetailsScreen(
                transactionId = id,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onEdit = {
                    navController.navigate(Screen.EditTransaction.createRoute(it))
                }
            )
        }
        composable(Screen.Debts.route) {
            DebtsScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onCustomerClick = {
                    navController.navigate(Screen.CustomerDetails.createRoute(it))
                }
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen(onNavigateUp = {
                navController.navigateUp()
            })
        }
        composable(Screen.PaymentMethods.route) {
            PaymentMethodsScreen(onNavigateUp = {
                navController.navigateUp()
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onNavigateToPaymentMethods = {
                    navController.navigate(Screen.PaymentMethods.route)
                }
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen(onNavigateUp = {
                navController.navigateUp()
            })
        }
    }
}