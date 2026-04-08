package com.trader.admin.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.trader.admin.ui.auth.LoginScreen
import com.trader.admin.ui.auth.AuthViewModel
import com.trader.admin.ui.dashboard.DashboardScreen
import com.trader.admin.ui.merchants.list.MerchantsScreen
import com.trader.admin.ui.merchants.add.AddMerchantScreen
import com.trader.admin.ui.merchants.detail.MerchantDetailScreen
import com.trader.admin.ui.chat.list.ChatListScreen
import com.trader.admin.ui.chat.detail.ChatDetailScreen
import org.koin.androidx.compose.koinViewModel

sealed class AdminScreen(val route: String) {
    object Login          : AdminScreen("login")
    object Dashboard      : AdminScreen("dashboard")
    object Merchants      : AdminScreen("merchants")
    object AddMerchant    : AdminScreen("merchants/add")
    object MerchantDetail : AdminScreen("merchants/{merchantId}") {
        fun route(id: String) = "merchants/$id"
    }
    object ChatList       : AdminScreen("chat")
    object ChatDetail     : AdminScreen("chat/{activationCode}?name={merchantName}") {
        // ✅ use activationCode — same as what merchant app uses
        fun route(activationCode: String, name: String) = "chat/$activationCode?name=$name"
    }
}

@Composable
fun AdminNavigation() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = koinViewModel()
    val authState by authVm.state.collectAsState()

    val start = if (authState.isAuthenticated) AdminScreen.Dashboard.route
                else AdminScreen.Login.route

    NavHost(navController = navController, startDestination = start) {
        composable(AdminScreen.Login.route) {
            LoginScreen(onAuthenticated = {
                navController.navigate(AdminScreen.Dashboard.route) {
                    popUpTo(AdminScreen.Login.route) { inclusive = true }
                }
            })
        }
        composable(AdminScreen.Dashboard.route) {
            DashboardScreen(
                onNavigateToMerchants = { navController.navigate(AdminScreen.Merchants.route) },
                onNavigateToChat      = { navController.navigate(AdminScreen.ChatList.route) },
                onSignOut             = {
                    authVm.signOut()
                    navController.navigate(AdminScreen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(AdminScreen.Merchants.route) {
            MerchantsScreen(
                onNavigateUp    = { navController.navigateUp() },
                onMerchantClick = { navController.navigate(AdminScreen.MerchantDetail.route(it)) },
                onAddMerchant   = { navController.navigate(AdminScreen.AddMerchant.route) }
            )
        }
        composable(AdminScreen.AddMerchant.route) {
            AddMerchantScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable(
            AdminScreen.MerchantDetail.route,
            listOf(navArgument("merchantId") { type = NavType.StringType })
        ) {
            MerchantDetailScreen(
                merchantId   = it.arguments!!.getString("merchantId")!!,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(AdminScreen.ChatList.route) {
            ChatListScreen(
                onNavigateUp = { navController.navigateUp() },
                // ✅ activationCode passed — matches merchant's Firestore path
                onChatClick  = { code, name ->
                    navController.navigate(AdminScreen.ChatDetail.route(code, name))
                }
            )
        }
        composable(
            AdminScreen.ChatDetail.route,
            listOf(
                navArgument("activationCode") { type = NavType.StringType },
                navArgument("merchantName")   { type = NavType.StringType; defaultValue = "بائع" }
            )
        ) {
            ChatDetailScreen(
                merchantId   = it.arguments!!.getString("activationCode")!!,
                merchantName = it.arguments!!.getString("merchantName") ?: "بائع",
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
