package com.trader.admin.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.trader.admin.ui.auth.AuthViewModel
import com.trader.admin.ui.auth.LoginScreen
import com.trader.admin.ui.chat.detail.ChatDetailScreen
import com.trader.admin.ui.chat.detail.ChatDetailViewModel
import com.trader.admin.ui.chat.list.ChatListScreen
import com.trader.admin.ui.chat.list.ChatListViewModel
import com.trader.admin.ui.dashboard.DashboardScreen
import com.trader.admin.ui.dashboard.DashboardViewModel
import com.trader.admin.ui.merchants.add.AddMerchantScreen
import com.trader.admin.ui.merchants.add.AddMerchantViewModel
import com.trader.admin.ui.merchants.detail.MerchantDetailScreen
import com.trader.admin.ui.merchants.detail.MerchantDetailViewModel
import com.trader.admin.ui.merchants.list.MerchantsScreen
import com.trader.admin.ui.merchants.list.MerchantsViewModel
import com.trader.admin.ui.notifications.NotificationsScreen
import com.trader.admin.ui.notifications.NotificationsViewModel
import com.trader.admin.ui.requests.AdminRequestsScreen
import com.trader.admin.ui.requests.AdminRequestsViewModel
import com.trader.admin.ui.subscriptionsettings.AdminSubscriptionScreen
import com.trader.admin.ui.subscriptionsettings.AdminSubscriptionViewModel
import com.trader.admin.ui.theme.AdminTheme
import com.trader.testing.recomposition.FakeViewModels.fake
import com.trader.testing.recomposition.RecompositionMeter
import com.trader.testing.recomposition.RecompositionTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Recomposition audit of every admin screen (open / 10 s idle / one interaction) with the real
 * admin theme. Admin screens have no infinite animations, so idle must be exactly zero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "ar-w360dp-h800dp")
class AdminRecompositionAuditTest {
    // Admin unit tests run on the release variant (google-services.json has no ".debug" client),
    // which does not merge ui-test-manifest, so the host activity is registered here.
    @get:Rule(order = 0) val registerActivity = AdminTestActivity.rule()
    @get:Rule(order = 1) val ui = createAndroidComposeRule<ComponentActivity>()
    private val meter = RecompositionMeter(ui)

    @After fun stop() = RecompositionTracker.stop()

    private fun audit(name: String, interaction: (() -> Unit)? = RecompositionMeter.DEFAULT, content: @Composable () -> Unit) {
        val m = meter.measure(name, { ui.setContent { AdminTheme { content() } } }, interaction)
        assertEquals("$name: idle frames must not recompose anything: ${m.idle.hot}", 0, m.idle.total)
    }

    @Test fun login() { val (vm, _) = fake<AuthViewModel>(); audit("admin-login") { LoginScreen({}, vm) } }
    @Test fun dashboard() { val (vm, _) = fake<DashboardViewModel>(); audit("admin-dashboard") { DashboardScreen({}, {}, {}, {}, {}, {}, vm) } }
    @Test fun merchants() { val (vm, _) = fake<MerchantsViewModel>(); audit("admin-merchants") { MerchantsScreen({}, {}, {}, vm) } }
    @Test fun merchantDetail() { val (vm, _) = fake<MerchantDetailViewModel>(); audit("admin-merchant-detail") { MerchantDetailScreen("m", {}, vm) } }
    @Test fun addMerchant() { val (vm, _) = fake<AddMerchantViewModel>(); audit("admin-add-merchant") { AddMerchantScreen({}, vm) } }
    @Test fun requests() { val (vm, _) = fake<AdminRequestsViewModel>(); audit("admin-requests") { AdminRequestsScreen({}, vm) } }
    @Test fun subscription() { val (vm, _) = fake<AdminSubscriptionViewModel>(); audit("admin-subscription") { AdminSubscriptionScreen({}, vm) } }
    @Test fun chatList() { val (vm, _) = fake<ChatListViewModel>(); audit("admin-chat-list") { ChatListScreen({}, { _, _ -> }, vm) } }
    @Test fun chatDetail() { val (vm, _) = fake<ChatDetailViewModel>(); audit("admin-chat-detail") { ChatDetailScreen("m", "متجر", {}, vm) } }
    @Test fun notifications() { val (vm, _) = fake<NotificationsViewModel>(); audit("admin-notifications") { NotificationsScreen({}, vm) } }
}
