package com.trader.salesmanager.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.trader.salesmanager.ui.reports.DayTransactionsScreen
import com.trader.salesmanager.ui.returns.ReturnProcessScreen
import com.trader.salesmanager.ui.inventory.list.InventoryListScreen
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductScreen
import com.trader.salesmanager.ui.inventory.detail.ProductDetailScreen
import com.trader.salesmanager.ui.inventory.invoice.InvoiceItemsScreen
import com.trader.salesmanager.ui.inventory.invoice.InvoiceLineItem
import com.trader.salesmanager.ui.inventory.invoice.SaleWeightUnit
import com.trader.salesmanager.ui.inventory.reports.StockReportsScreen
import org.json.JSONArray
import org.json.JSONObject
import com.trader.salesmanager.ui.settings.SettingsScreen
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionScreen
import com.trader.salesmanager.ui.transactions.details.TransactionDetailsScreen
import com.trader.salesmanager.ui.transactions.list.TransactionsScreen
import org.koin.androidx.compose.koinViewModel

// تسلسل خطوط الفاتورة لنقلها عبر SavedStateHandle
// ✅ يحفظ displayQty + displayWeightUnit حتى يتم إعادة بناء InvoiceLineItem بشكل صحيح
private fun serializeLines(lines: List<InvoiceLineItem>): String {
    val arr = JSONArray()
    lines.forEach {
        line ->
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
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventory.route)
                },
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onTransactionClick = {
                    id -> navController.navigate(Screen.TransactionDetails.createRoute(id))
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

            val invoiceViewModel: com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel =
            org.koin.androidx.compose.koinViewModel()

            val linesJson by back.savedStateHandle
            .getStateFlow<String?>("invoice_lines_json", null)
            .collectAsState()

            // ✅ إصلاح race condition:
            // invoiceTotal كان يُعيَّن بعد linesJson فيخرج LaunchedEffect مبكراً.
            // applyInvoiceLinesFromJson تحسب المبلغ داخلياً → لا نحتاج total من الخارج.
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
                onNavigateToInvoiceItems = {
                    customerName, existingLinesJson ->
                    // ✅ نكتب في currentBackStackEntry (AddTransaction) قبل navigate
                    // InvoiceItems ستقرأ من previousBackStackEntry → هذه الشاشة
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
        ) {
            back ->
            val invoiceViewModel: com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel =
            org.koin.androidx.compose.koinViewModel()

            val linesJson by back.savedStateHandle
            .getStateFlow<String?>("invoice_lines_json", null)
            .collectAsState()

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
                onNavigateToInvoiceItems = {
                    customerName, existingLinesJson ->
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
                },
                onNavigateToReturn = {
                    txId ->
                    // هذا هو السطر الذي يفتح شاشة المرتجعات
                    navController.navigate(Screen.ReturnProcess.createRoute(txId)
                    )
                })
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
                ReportsScreen(
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onViewDayTransactions = {
                        dateMillis ->
                        navController.navigate(Screen.DayTransactions.createRoute(dateMillis))
                    }
                )
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
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.Chat.route)
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
            ) {
                back ->
                val dateMillis = back.arguments!!.getLong("dateMillis")
                DayTransactionsScreen(
                    dateMillis = dateMillis,
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onTransactionClick = {
                        id -> navController.navigate(Screen.TransactionDetails.createRoute(id))
                    }
                )
            }
            // ── v2 — المخزن ──────────────────────────────────────────
            composable(Screen.Inventory.route) {
                InventoryListScreen(
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onProductClick = {
                        id -> navController.navigate(Screen.ProductDetail.createRoute(id))
                    },
                    onAddProduct = {
                        barcode -> navController.navigate(Screen.AddProduct.createRoute(barcode))
                    },
                    onInventorySession = {
                        navController.navigate(Screen.InventorySession.route)
                    },
                    onStockReports = {
                        navController.navigate(Screen.StockReports.route)
                    }
                )
            }
            composable(
                Screen.AddProduct.route,
                listOf(navArgument("barcode") {
                    type = NavType.StringType; defaultValue = ""
                })
            ) {
                back ->
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
            ) {
                back ->
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
            ) {
                back ->
                val productId = back.arguments!!.getString("productId")!!
                ProductDetailScreen(
                    productId = productId,
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onEdit = {
                        id -> navController.navigate(Screen.EditProduct.createRoute(id))
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
            ) {
                back ->
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
                    onConfirm = {
                        lines, total ->
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
            ) {
                back ->
                ReturnProcessScreen(
                    transactionId = back.arguments!!.getLong("transactionId"),
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onReturnSuccess = {
                        navController.popBackStack()
                        navController.popBackStack() // يرجع لشاشة التفاصيل
                    }
                )
            }
        }
    }

}