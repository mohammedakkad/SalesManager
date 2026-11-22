package com.trader.admin.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.trader.admin.ui.auth.AuthViewModel
import com.trader.admin.ui.auth.LoginScreen
import com.trader.admin.ui.dashboard.DashboardScreen
import com.trader.admin.ui.dashboard.DashboardViewModel
import com.trader.admin.ui.merchants.detail.MerchantDetailScreen
import com.trader.admin.ui.merchants.detail.MerchantDetailViewModel
import com.trader.admin.ui.merchants.list.MerchantsScreen
import com.trader.admin.ui.merchants.list.MerchantsViewModel
import com.trader.admin.ui.theme.AdminTheme
import com.trader.testing.recomposition.ClickableAccessibility
import com.trader.testing.recomposition.FakeViewModels.fake
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Icon-only buttons are announced by TalkBack and every clickable target is at least 48 dp. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "ar-w360dp-h800dp")
class AdminAccessibilityTest {
    @get:Rule(order = 0) val registerActivity = AdminTestActivity.rule()
    @get:Rule(order = 1) val ui = createAndroidComposeRule<ComponentActivity>()

    private fun check(name: String, content: @Composable () -> Unit) {
        ui.setContent { AdminTheme { content() } }
        ui.waitForIdle()
        val problems = ClickableAccessibility.violations(ui)
        println("A11Y $name ${problems.size}: $problems")
        assertTrue("$name:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    @Test fun login() { val (vm, _) = fake<AuthViewModel>(); check("login") { LoginScreen({}, vm) } }
    @Test fun dashboard() { val (vm, _) = fake<DashboardViewModel>(); check("dashboard") { DashboardScreen({}, {}, {}, {}, {}, {}, vm) } }
    @Test fun merchants() { val (vm, _) = fake<MerchantsViewModel>(); check("merchants") { MerchantsScreen({}, {}, {}, vm) } }
    @Test fun merchantDetail() { val (vm, _) = fake<MerchantDetailViewModel>(); check("merchant-detail") { MerchantDetailScreen("m", {}, vm) } }
}
