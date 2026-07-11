package com.trader.salesmanager.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trader.salesmanager.ui.activation.ActivationScreen
import com.trader.salesmanager.ui.activation.ActivationViewModel
import com.trader.salesmanager.ui.activation.MerchantEvent
import com.trader.salesmanager.ui.activation.MerchantWatcherViewModel
import com.trader.salesmanager.ui.activation.StartupState
import com.trader.salesmanager.ui.boxes.BoxesScreen
import com.trader.salesmanager.ui.chat.ChatScreen
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerScreen
import com.trader.salesmanager.ui.customers.details.CustomerDetailsScreen
import com.trader.salesmanager.ui.customers.list.CustomersScreen
import com.trader.salesmanager.ui.debts.DebtsScreen
import com.trader.salesmanager.ui.employees.EmployeeManagementScreen
import com.trader.salesmanager.ui.home.HomeScreen
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductScreen
import com.trader.salesmanager.ui.inventory.detail.ProductDetailScreen
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsScreen
import com.trader.salesmanager.ui.inventory.invoice.InvoiceLineItem
import com.trader.salesmanager.ui.inventory.list.InventoryListScreen
import com.trader.salesmanager.ui.inventory.reports.StockReportsScreen
import com.trader.salesmanager.ui.payments.PaymentMethodsScreen
import com.trader.salesmanager.ui.reports.DayTransactionsScreen
import com.trader.salesmanager.ui.reports.ReportsScreen
import com.trader.salesmanager.ui.returns.ReturnProcessScreen
import com.trader.salesmanager.ui.settings.SettingsScreen
import com.trader.salesmanager.ui.settings.backup.BackupScreen
import com.trader.salesmanager.ui.settings.sessions.SessionsScreen
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionScreen
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsScreen
import com.trader.salesmanager.ui.transactions.list.TransactionsScreen
import org.json.JSONArray
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

// تسلسل خطوط الفاتورة لنقلها عبر SavedStateHandle
// ✅ يحفظ displayQty + displayWeightUnit حتى يتم إعادة بناء InvoiceLineItem بشكل صحيح
private fun serializeLines(lines: List<InvoiceLineItem>): String {
    val arr = JSONArray()
    lines.forEach { line ->
        arr.put(JSONObject().apply {
            put("productId", line.product.product.id)
            put("productName", line.product.product.name)
            put("unitId", line.selectedUnit.id)
            put("unitLabel", line.selectedUnit.unitLabel)
            put("displayQty", line.displayQty) // الكمية كما أدخلها البائع
            put("displayWeightUnit", line.displayWeightUnit.name) // KG / GRAM / OZ / POUND
            put("price", line.effectivePrice)
            // quantity (بالكيلو) لا نحفظها — تُحسب تلقائياً عند إعادة البناء
        })
    }
    return arr.toString()
}

@Composable
fun AppNavigation(
    pendingNavigation: String? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val activationVm: ActivationViewModel = koinViewModel()
    val watcherVm: MerchantWatcherViewModel = koinViewModel()

    val startupState by activationVm.startupState.collectAsStateWithLifecycle()
    var liveBlockMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        watcherVm.event.collect { event ->
            val msg = when (event) {
                is MerchantEvent.Disabled -> "تم تعطيل حسابك من قِبل الإدارة."
                is MerchantEvent.Deleted -> "تم حذف حسابك. تواصل مع الإدارة."
                is MerchantEvent.Expired -> "انتهت مدة اشتراكك. تواصل مع الإدارة للتجديد."
                is MerchantEvent.ExpiryWarning -> null
            }
            if (msg != null) liveBlockMessage = msg
        }
    }

    liveBlockMessage?.let { msg ->
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
        }

        else -> {
            /* ACTIVE or NeedActivation — continue to NavHost */
        }
    }

    val start = when {
        startupState != StartupState.Proceed && startupState !is StartupState.ProceedFree ->
            Screen.Activation.route
        else -> Screen.Home.route
    }

    LaunchedEffect(pendingNavigation, startupState) {
        if (pendingNavigation == com.trader.salesmanager.MainActivity.NAV_SETTINGS &&
            (startupState == StartupState.Proceed || startupState is StartupState.ProceedFree)
        ) {
            navController.navigate(Screen.Settings.route)
            onNavigationHandled()
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomNavigation = isMainBottomNavRoute(currentRoute)
    var homeBottomBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.Home.route) {
            homeBottomBarVisible = true
        }
    }

    BackHandler(
        enabled = showBottomNavigation && currentRoute != Screen.Home.route
    ) {
        navController.navigateToMainTab(Screen.Home.route, currentRoute)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomNavigation) {
                    MainBottomNavigationBarHost(
                        currentRoute = currentRoute,
                        homeBarVisible = homeBottomBarVisible,
                        onTabSelected = { route ->
                            navController.navigateToMainTab(route, currentRoute)
                        }
                    )
                }
            }
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = start,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
                enterTransition = {
                    if (
                        isMainBottomNavRoute(initialState.destination.route) &&
                        isMainBottomNavRoute(targetState.destination.route)
                    ) {
                        fadeIn(tween(220))
                    } else {
                        slideInHorizontally(tween(280)) {
                            it / 4
                        } + fadeIn(tween(280))
                    }
                },
                exitTransition = {
                    if (
                        isMainBottomNavRoute(initialState.destination.route) &&
                        isMainBottomNavRoute(targetState.destination.route)
                    ) {
                        fadeOut(tween(220))
                    } else {
                        slideOutHorizontally(tween(280)) {
                            -it / 4
                        } + fadeOut(tween(280))
                    }
                },
                popEnterTransition = {
                    if (
                        isMainBottomNavRoute(initialState.destination.route) &&
                        isMainBottomNavRoute(targetState.destination.route)
                    ) {
                        fadeIn(tween(220))
                    } else {
                        slideInHorizontally(tween(280)) {
                            -it / 4
                        } + fadeIn(tween(280))
                    }
                },
                popExitTransition = {
                    if (
                        isMainBottomNavRoute(initialState.destination.route) &&
                        isMainBottomNavRoute(targetState.destination.route)
                    ) {
                        fadeOut(tween(220))
                    } else {
                        slideOutHorizontally(tween(280)) {
                            it / 4
                        } + fadeOut(tween(280))
                    }
                }
            ) {
        composable(Screen.Activation.route) {
            ActivationScreen(
                onActivated = {
                    activationVm.checkStartup()
                },
                // هذه هي الميزة المطلوبة من الملف الثاني
                onFreeStart = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Activation.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTransactions = {
                    navController.navigate(Screen.TransactionsList.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onTransactionClick = { id ->
                    navController.navigate(Screen.TransactionDetails.createRoute(id))
                },
                onCustomerClick = { id ->
                    navController.navigate(Screen.CustomerDetails.createRoute(id))
                },
                onBottomBarVisibilityChanged = { visible ->
                    if (homeBottomBarVisible != visible) {
                        homeBottomBarVisible = visible
                    }
                }
            )
        }
        composable(Screen.CustomersList.route) {
            CustomersScreen(
                onNavigateUp = {
                    navController.navigateToMainTab(Screen.Home.route, currentRoute)
                },
                onCustomerClick = {
                    navController.navigate(Screen.CustomerDetails.createRoute(it))
                },
                onAddCustomer = {
                    navController.navigate(Screen.AddCustomer.route)
                },
                showNavigateUp = false
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
        ) { back ->
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
        ) { back ->
            val preselectedId = back.arguments!!.getLong("customerId").takeIf {
                it != -1L
            }

            val invoiceViewModel: com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel =
                koinViewModel()

            val linesJson by back.savedStateHandle
                .getStateFlow<String?>("invoice_lines_json", null)
                .collectAsStateWithLifecycle()

            LaunchedEffect(linesJson) {
                val json = linesJson ?: return@LaunchedEffect
                invoiceViewModel.applyInvoiceLinesFromJson(json)
                back.savedStateHandle.remove<String>("invoice_lines_json")
            }

            AddEditTransactionScreen(
                transactionId = null,
                preselectedCustomerId = preselectedId,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onNavigateToInvoiceItems = { customerName, existingLinesJson ->

                    if (existingLinesJson != null) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("existing_lines_json", existingLinesJson)
                    } else {
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.remove<String>("existing_lines_json")
                    }
                    navController.navigate(Screen.InvoiceItems.createRoute(customerName))
                },
                viewModel = invoiceViewModel
            )
        }
        composable(
            Screen.EditTransaction.route,
            listOf(navArgument("transactionId") {
                type = NavType.LongType
            })
        ) { back ->
            val invoiceViewModel: com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel =
                koinViewModel()

            val linesJson by back.savedStateHandle
                .getStateFlow<String?>("invoice_lines_json", null)
                .collectAsStateWithLifecycle()

            LaunchedEffect(linesJson) {
                val json = linesJson ?: return@LaunchedEffect
                invoiceViewModel.applyInvoiceLinesFromJson(json)
                back.savedStateHandle.remove<String>("invoice_lines_json")
            }

            AddEditTransactionScreen(
                transactionId = back.arguments!!.getLong("transactionId"),
                preselectedCustomerId = null,
                onNavigateUp = {
                    navController.navigateUp()
                },
                // ✅ أضف onNavigateToInvoiceItems
                onNavigateToInvoiceItems = { customerName, existingLinesJson ->
                    if (existingLinesJson != null) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("existing_lines_json", existingLinesJson)
                    } else {
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.remove<String>("existing_lines_json")
                    }
                    navController.navigate(Screen.InvoiceItems.createRoute(customerName))
                },
                viewModel = invoiceViewModel
            )
        }
        composable(
            Screen.TransactionDetails.route,
            listOf(navArgument("transactionId") {
                type = NavType.LongType
            })
        ) { back ->
            val id = back.arguments!!.getLong("transactionId")
            TransactionDetailsScreen(
                transactionId = id,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onEdit = {
                    navController.navigate(Screen.EditTransaction.createRoute(it))
                },
                onNavigateToReturn = { txId ->
                    // هذا هو السطر الذي يفتح شاشة المرتجعات
                    navController.navigate(
                        Screen.ReturnProcess.createRoute(txId)
                    )
                })
        }
        composable(Screen.Debts.route) {
            DebtsScreen(
                onNavigateUp = {
                    navController.navigateToMainTab(Screen.Home.route, currentRoute)
                },
                onCustomerClick = {
                    navController.navigate(Screen.CustomerDetails.createRoute(it))
                },
                showNavigateUp = false
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateUp = {
                    navController.navigateToMainTab(Screen.Home.route, currentRoute)
                },
                onViewDayTransactions = { dateMillis ->
                    navController.navigate(Screen.DayTransactions.createRoute(dateMillis))
                },
                showNavigateUp = false
            )
        }
        composable(Screen.PaymentMethods.route) {
            PaymentMethodsScreen(onNavigateUp = {
                navController.navigateUp()
            })
        }
        composable(Screen.CashBoxes.route) {
            BoxesScreen(onNavigateUp = {
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
                },
                onNavigateToCashBoxes = {
                    navController.navigate(Screen.CashBoxes.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                }
            )
        }
        composable(Screen.Backup.route) {
            BackupScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable(Screen.Sessions.route) {
            SessionsScreen(
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen(onNavigateUp = {
                navController.navigateUp()
            })
        }
        composable(
            Screen.DayTransactions.route,
            listOf(navArgument("dateMillis") {
                type = NavType.LongType
            })
        ) { back ->
            val dateMillis = back.arguments!!.getLong("dateMillis")
            DayTransactionsScreen(
                dateMillis = dateMillis,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onTransactionClick = { id ->
                    navController.navigate(Screen.TransactionDetails.createRoute(id))
                }
            )
        }
        // ── v2 — المخزن ──────────────────────────────────────────
        composable(Screen.Inventory.route) {
            InventoryListScreen(
                onNavigateUp = {
                    navController.navigateToMainTab(Screen.Home.route, currentRoute)
                },
                onProductClick = { id ->
                    navController.navigate(Screen.ProductDetail.createRoute(id))
                },
                onAddProduct = { barcode ->
                    navController.navigate(Screen.AddProduct.createRoute(barcode))
                },
                onInventorySession = {
                    navController.navigate(Screen.InventorySession.route)
                },
                onStockReports = {
                    navController.navigate(Screen.StockReports.route)
                },
                showNavigateUp = false
            )
        }
        composable(
            Screen.AddProduct.route,
            listOf(navArgument("barcode") {
                type = NavType.StringType; defaultValue = ""
            })
        ) { back ->
            val barcode = back.arguments?.getString("barcode")?.ifEmpty {
                null
            }
            AddEditProductScreen(
                productId = null, initialBarcode = barcode,
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            Screen.EditProduct.route,
            listOf(navArgument("productId") {
                type = NavType.StringType
            })
        ) { back ->
            AddEditProductScreen(
                productId = back.arguments!!.getString("productId"),
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            Screen.ProductDetail.route,
            listOf(navArgument("productId") {
                type = NavType.StringType
            })
        ) { back ->
            val productId = back.arguments!!.getString("productId")!!
            ProductDetailScreen(
                productId = productId,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onEdit = { id ->
                    navController.navigate(Screen.EditProduct.createRoute(id))
                }
            )
        }
        composable(Screen.InventorySession.route) {
            com.trader.salesmanager.ui.inventory.session.InventorySessionScreen(
                onNavigateUp = {
                    navController.navigateUp()
                }
            )
        }
        composable(Screen.StockReports.route) {
            StockReportsScreen(onNavigateUp = {
                navController.navigateUp()
            })
        }
        composable(
            Screen.InvoiceItems.route,
            listOf(navArgument("customerName") {
                type = NavType.StringType
            })
        ) { back ->
            val customerName = back.arguments?.getString("customerName")
                ?.let {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } ?: ""
            val existingLinesJson = navController.previousBackStackEntry
                ?.savedStateHandle?.get<String>("existing_lines_json")
            InvoiceItemsScreen(
                customerName = customerName,
                existingLinesJson = existingLinesJson,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onConfirm = { lines, total ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.remove<String>("existing_lines_json")
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set("invoice_lines_json", serializeLines(lines))
                    navController.navigateUp()
                }
            )
        }

        composable(
            route = Screen.ReturnProcess.route,
            arguments = listOf(navArgument("transactionId") {
                type = NavType.LongType
            })
        ) { back ->
            val txId = back.arguments!!.getLong("transactionId")
            ReturnProcessScreen(
                transactionId = txId,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onReturnSuccess = {
                    // ✅ يرجع لشاشة التفاصيل مباشرة
                    navController.popBackStack()
                }
            )
        }

        // ── Phase 4.3 — RBAC / Employees ─────────────────────────
        composable(Screen.EmployeeManagement.route) {
            EmployeeManagementScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
            }
        }
    }
}