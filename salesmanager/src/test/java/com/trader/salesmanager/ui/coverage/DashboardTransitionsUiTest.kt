package com.trader.salesmanager.ui.coverage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.home.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h800dp")
class DashboardTransitionsUiTest:OfflineSellerUiTest() {
    @Test fun productRankingCanLoadReplaceAndBecomeEmptyWithoutStaleRows(){
        val products=mutableStateOf(emptyList<TopSellingProduct>())
        show{TopProductsCard(products.value)}
        ui.onNodeWithText("لا توجد مبيعات أصناف هذا الشهر بعد").assertExists()
        ui.runOnIdle{products.value=listOf(TopSellingProduct("p","قهوة",1250.5),TopSellingProduct("q","شاي",2.0))}
        ui.onNodeWithText("قهوة").assertExists();ui.onNodeWithText("1,250.5 مباع").assertExists()
        ui.onNodeWithText("شاي").assertExists();ui.onNodeWithText("2 مباع").assertExists()
        ui.onNodeWithText("لا توجد مبيعات أصناف هذا الشهر بعد").assertDoesNotExist()
        ui.runOnIdle{products.value=listOf(TopSellingProduct("r","سكر",0.5))}
        ui.onNodeWithText("سكر").assertExists();ui.onNodeWithText("0.5 مباع").assertExists();ui.onNodeWithText("قهوة").assertDoesNotExist()
        ui.runOnIdle{products.value=emptyList()}
        ui.onNodeWithText("لا توجد مبيعات أصناف هذا الشهر بعد").assertExists();ui.onNodeWithText("سكر").assertDoesNotExist()
    }
    @Test fun debtorRankingReorderingNavigatesUsingTheCurrentIdentity(){
        val debtors=mutableStateOf(listOf(TopDebtorCustomer(7,"أول",1200.0),TopDebtorCustomer(9,"ثان",300.0)))
        val selected=mutableListOf<Long>()
        show{TopDebtorsCard(debtors.value,{selected+=it})}
        ui.onNodeWithText("ثان").performClick();assertEquals(listOf(9L),selected)
        ui.runOnIdle{debtors.value=listOf(TopDebtorCustomer(13,"جديد",900.0),TopDebtorCustomer(7,"أول",200.0))}
        ui.onNodeWithText("ثان").assertDoesNotExist();ui.onNodeWithText("₪ 1,200").assertDoesNotExist()
        ui.onNodeWithText("جديد").performClick();assertEquals(listOf(9L,13L),selected)
        ui.onNodeWithText("₪ 900").assertExists();ui.onNodeWithText("₪ 200").assertExists()
    }
    @Test fun clearingDebtorsAfterSettlementRemovesClickTargetsAndShowsEmptyState(){
        val debtors=mutableStateOf(listOf(TopDebtorCustomer(7,"مدين",0.5)))
        var calls=0
        show{TopDebtorsCard(debtors.value,{calls++})}
        ui.onNodeWithText("مدين").assertExists()
        ui.runOnIdle{debtors.value=emptyList()}
        ui.onNodeWithText("لا توجد ديون مستحقة حالياً").assertExists();ui.onNodeWithText("مدين").assertDoesNotExist();assertEquals(0,calls)
    }
    @Test fun wideIndicatorsKeepAllThreeValuesOnOneRowAndRefreshTheirNumbers(){
        val sales=mutableStateOf(1200.0);val invoices=mutableStateOf(7);val debt=mutableStateOf(300.0)
        show{Box(Modifier.width(380.dp)){TopIndicatorsRow(sales.value,invoices.value,debt.value)}}
        val a=ui.onNodeWithText("مبيعات اليوم").fetchSemanticsNode().boundsInRoot
        val b=ui.onNodeWithText("عدد الفواتير اليوم").fetchSemanticsNode().boundsInRoot
        val c=ui.onNodeWithText("إجمالي الديون").fetchSemanticsNode().boundsInRoot
        assertEquals(a.top,b.top,1f);assertEquals(a.top,c.top,1f)
        ui.runOnIdle{sales.value=2400.0;invoices.value=9;debt.value=0.0};ui.mainClock.advanceTimeBy(6000)
        ui.onNodeWithText("₪ 2,400").assertExists();ui.onNodeWithText("9").assertExists();ui.onNodeWithText("₪ 0").assertExists()
        ui.onNodeWithText("₪ 1,200").assertDoesNotExist()
    }
    @Test fun narrowIndicatorsWrapDebtToASecondRowWithoutDroppingItsValue(){
        show{Box(Modifier.width(320.dp)){TopIndicatorsRow(1200.0,7,300.0)}}
        val sales=ui.onNodeWithText("مبيعات اليوم").fetchSemanticsNode().boundsInRoot
        val count=ui.onNodeWithText("عدد الفواتير اليوم").fetchSemanticsNode().boundsInRoot
        val debt=ui.onNodeWithText("إجمالي الديون").fetchSemanticsNode().boundsInRoot
        assertEquals(sales.top,count.top,1f);assertTrue(debt.top>sales.bottom)
        ui.onNodeWithText("₪ 300").assertExists();ui.onNodeWithText("7").assertExists()
    }
}
