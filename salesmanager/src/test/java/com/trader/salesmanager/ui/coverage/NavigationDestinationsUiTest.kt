package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.core.sync.GlobalSyncState
import com.trader.salesmanager.MainActivity
import com.trader.salesmanager.ui.activation.*
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.debts.*
import com.trader.salesmanager.ui.inventory.list.*
import com.trader.salesmanager.ui.inventory.invoice.*
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.navigation.AppNavigation
import com.trader.salesmanager.ui.boxes.*
import com.trader.salesmanager.ui.payments.*
import com.trader.salesmanager.ui.chat.*
import com.trader.salesmanager.ui.settings.backup.*
import com.trader.salesmanager.ui.transactions.addedit.*
import com.trader.salesmanager.update.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Real NavHost, notification entry points and invoice SavedStateHandle round trips; no real backend. */
@RunWith(RobolectricTestRunner::class)
class NavigationDestinationsUiTest:OfflineNavigationFixture() {
    @Test fun settingsNotificationAcknowledgesExactlyOnceAndOpensCashDestination() {
        var handled=0;show{AppNavigation(MainActivity.NAV_SETTINGS){handled++}}
        ui.onNodeWithText("الصناديق").performScrollTo().performClick()
        ui.onNodeWithText("الرصيد الإجمالي").assertExists();assertEquals(1,handled)
    }
    @Test fun settingsNotificationOpensPaymentMethodDestination() {
        show{AppNavigation(MainActivity.NAV_SETTINGS)}
        ui.onNodeWithText("طرق الدفع").performScrollTo().performClick()
        ui.onNodeWithText("طريقة المسار").assertExists()
    }
    @Test fun settingsNotificationOpensBackupDestination() {
        show{AppNavigation(MainActivity.NAV_SETTINGS)}
        ui.onNodeWithText("النسخ الاحتياطي").performScrollTo().performClick()
        ui.onNodeWithText("النسخ الاحتياطي").assertExists()
        ui.onNode(hasText("الرئيسية") and isSelectable()).assertDoesNotExist()
    }
    @Test fun settingsNotificationOpensTheSupportConversation() {
        show{AppNavigation(MainActivity.NAV_SETTINGS)}
        ui.onNodeWithText("الدعم الفني").performScrollTo().performClick()
        ui.onNodeWithText("دعم فني").assertExists()
    }
    @Test fun lowStockNotificationUsesTheAttentionFilterAndAcknowledgesOnlyOnce() {
        var handled=0;show{AppNavigation(MainActivity.NAV_LOW_STOCK){handled++}}
        ui.onNode(hasText("المخزن") and isSelectable()).assertIsSelected()
        verify(exactly=1){stock.setFilter(StockFilter.ATTENTION)};assertEquals(1,handled)
    }
    @Test fun unknownNotificationDoesNotNavigateOrAcknowledgeAnUnrecognizedAction() {
        var handled=0;show{AppNavigation("unknown-offline-action"){handled++}}
        ui.onNodeWithText("مدير المبيعات").assertExists();assertEquals(0,handled)
    }
    @Test fun notificationCannotBypassAnUnactivatedStartup() {
        startup.value=StartupState.NeedActivation;var handled=0
        show{AppNavigation(MainActivity.NAV_SETTINGS){handled++}}
        ui.onNodeWithText("كود التفعيل").assertExists();assertEquals(0,handled)
        ui.onNode(hasText("المخزن") and isSelectable()).assertDoesNotExist()
    }
    @Test fun freeStartupUsesTheSameLowStockEntryContract() {
        startup.value=StartupState.ProceedFree;var handled=0
        show{AppNavigation(MainActivity.NAV_LOW_STOCK){handled++}}
        ui.onNode(hasText("المخزن") and isSelectable()).assertIsSelected();assertEquals(1,handled)
    }
    @Test fun aNonRetryableStartupBlockCannotOfferAFalseRetry() {
        startup.value=StartupState.Blocked("حساب محجوب",false);show{AppNavigation()}
        ui.onNodeWithText("حساب محجوب").assertExists();ui.onNodeWithText("إعادة المحاولة").assertDoesNotExist()
    }
    private fun block(event:MerchantEvent,message:String) {
        show{AppNavigation()};ui.runOnIdle{assertTrue(events.tryEmit(event))}
        ui.onNodeWithText(message).assertExists();ui.onNodeWithText("حسناً").performClick()
        verify(exactly=1){activation.deactivate()};ui.onNodeWithText("كود التفعيل").assertExists()
    }
    @Test fun deletedMerchantEventRoutesBackToActivationAfterAcknowledgment()=block(MerchantEvent.Deleted,"تم حذف حسابك. تواصل مع الإدارة.")
    @Test fun expiredMerchantEventRoutesBackToActivationAfterAcknowledgment()=block(MerchantEvent.Expired,"انتهت مدة اشتراكك. تواصل مع الإدارة للتجديد.")
    @Test fun homeSettingsActionHasAnAccessibleNameAndOpensSettings() {
        show{AppNavigation()};ui.onNodeWithContentDescription("الإعدادات").performClick()
        ui.onNodeWithText("اسم المحل").assertExists()
    }
    @Test fun homeSupportActionHasAnAccessibleNameAndOpensConversation() {
        show{AppNavigation()};ui.onNodeWithContentDescription("الدعم الفني").performClick()
        ui.onNodeWithText("دعم فني").assertExists()
    }
    private fun invoiceRoundTrip(name:String) {
        saleState.value=saleState.value.copy(selectedCustomer=customer.copy(name=name))
        show{AppNavigation()};ui.onNodeWithText("عملية جديدة",useUnmergedTree=true).assertIsDisplayed().performClick()
        ui.onNodeWithText("إضافة أصناف من المخزن (اختياري)").performClick()
        ui.onNodeWithText("الزبون: $name").assertExists()
        ui.onNode(hasText("تأكيد",substring=true) and hasClickAction()).assertIsDisplayed().performClick()
        ui.waitUntil(10000){ui.waitForIdle();appliedJson!=null}
        val json=slot<String>();verify(exactly=1){sale.applyInvoiceLinesFromJson(capture(json))}
        val row=JSONArray(json.captured).getJSONObject(0)
        assertEquals("p",row.getString("productId"));assertEquals("u",row.getString("unitId"));assertEquals("أرز المسار",row.getString("productName"))
        assertEquals(2.0,row.getDouble("displayQty"),0.0);assertEquals("POUND",row.getString("displayWeightUnit"));assertEquals(45.0,row.getDouble("price"),0.0)
        ui.onNodeWithText("إضافة أصناف من المخزن (اختياري)").assertExists()
    }
    @Test fun invoiceNavigationReturnsDisplayWeightAndCustomPriceThroughSavedState()=invoiceRoundTrip("زبون مسار")
    @Test fun invoiceNavigationPreservesLiteralPlusPercentAndSlashInCustomerName()=invoiceRoundTrip("زبون + 50% / س")
}
