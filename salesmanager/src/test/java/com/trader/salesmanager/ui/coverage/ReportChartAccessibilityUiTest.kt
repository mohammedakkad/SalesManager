package com.trader.salesmanager.ui.coverage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.theme.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.*

/** Accessible values and actual native-rendered graph pixels; no synthetic reference images. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportChartAccessibilityUiTest:OfflineSellerUiTest() {
    private fun report(state:ReportsUiState,title:String){
        val vm=mockk<ReportsViewModel>(relaxed=true);every{vm.uiState} returns MutableStateFlow(state.copy(isLoading=false))
        show{ReportsScreen({},viewModel=vm)};ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title));ui.mainClock.advanceTimeBy(2000);ui.waitForIdle()
    }
    private fun countPixels(description:String,color:(Color)->Boolean):Int {
        val node=ui.onNodeWithContentDescription(description).assertExists().fetchSemanticsNode();val b=node.boundsInRoot
        lateinit var bitmap:Bitmap
        ui.runOnIdle{val view=activity.findViewById<View>(android.R.id.content);bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))}
        val pixels=bitmap.asImageBitmap().toPixelMap();var count=0
        for(x in floor(b.left).toInt().coerceAtLeast(0) until ceil(b.right).toInt().coerceAtMost(pixels.width))
            for(y in floor(b.top).toInt().coerceAtLeast(0) until ceil(b.bottom).toInt().coerceAtMost(pixels.height))if(color(pixels[x,y]))count++
        bitmap.recycle();return count
    }
    private fun near(target:Color):(Color)->Boolean = {p->abs(p.red-target.red)<.03f&&abs(p.green-target.green)<.03f&&abs(p.blue-target.blue)<.03f}
    @Test fun dailyCurveExposesEveryDayAmountAndDrawsItsActualMultiPointPath(){
        report(ReportsUiState(dailySales=listOf(DaySalesEntry("01/01",100.0,40.0),DaySalesEntry("02/01",40.0,20.0),DaySalesEntry("03/01",80.0,80.0))),"منحنى المبيعات اليومية")
        val d="المبيعات اليومية: 01/01: 100.00; 02/01: 40.00; 03/01: 80.00";assertTrue(countPixels(d,near(Emerald500))>20)
    }
    @Test fun paymentBarsExposePaidAndUnpaidAmountsAndDrawBothSeries(){
        report(ReportsUiState(dailySales=listOf(DaySalesEntry("01/01",100.0,30.0),DaySalesEntry("02/01",0.0,0.0))),"مدفوع مقابل غير مدفوع")
        val d="المدفوع وغير المدفوع: 01/01: مدفوع 30.00، غير مدفوع 70.00; 02/01: مدفوع 0.00، غير مدفوع 0.00"
        assertTrue(countPixels(d,near(PaidGreen))>20);assertTrue(countPixels(d){it.red>.7f&&it.red>it.green*1.4f&&it.red>it.blue*1.4f}>20)
    }
    @Test fun paymentDonutExposesAmountsAndDrawsTheMajorShareLarger(){
        report(ReportsUiState(paymentShares=listOf(PaymentShare("نقد",75.0),PaymentShare("دين",25.0))),"توزيع طرق الدفع")
        val d="طرق الدفع: نقد: 75.00; دين: 25.00";val green=countPixels(d,near(Emerald500));val blue=countPixels(d,near(Color(0xFF3B82F6)))
        assertTrue("Donut shares do not match their values: $green/$blue",blue>20&&green>blue*1.5)
    }
    @Test fun profitChartExposesSignedLossAndKeepsAllThreeSeriesVisible(){
        report(ReportsUiState(salesProfitLast7Days=listOf(SalesProfitDayEntry("01/01",100.0,-30.0),SalesProfitDayEntry("02/01",60.0,20.0))),"المبيعات مقابل الربح - آخر 7 أيام")
        val d="المبيعات والربح: 01/01: مبيعات 100.00، ربح -30.00; 02/01: مبيعات 60.00، ربح 20.00"
        for(c in listOf(Emerald500,Cyan500,DebtRed))assertTrue(countPixels(d,near(c))>20)
    }
    @Test fun singleZeroDayRemainsAccessibleAndDrawsAFinitePoint(){
        report(ReportsUiState(dailySales=listOf(DaySalesEntry("01/01",0.0,0.0))),"منحنى المبيعات اليومية")
        assertTrue(countPixels("المبيعات اليومية: 01/01: 0.00",near(Emerald500))>3)
    }
    @Test fun zeroPaymentSharesRemainAccessibleWithoutInventingAPositiveArc(){
        report(ReportsUiState(paymentShares=listOf(PaymentShare("نقد",0.0),PaymentShare("دين",0.0))),"توزيع طرق الدفع")
        val d="طرق الدفع: نقد: 0.00; دين: 0.00";assertEquals(0,countPixels(d,near(Emerald500)));assertEquals(0,countPixels(d,near(Color(0xFF3B82F6))))
    }
}
