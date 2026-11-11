package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File
import java.io.FileOutputStream
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.theme.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Native renderer pixels, not a claim of hardware/device screenshot qualification. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportCanvasRenderingUiTest:OfflineSellerUiTest() {
    private var fixtureName=""
    private fun chart(profit:Double){
        fixtureName=if(profit>=0)"positive" else "loss"
        val vm=mockk<ReportsViewModel>(relaxed=true);every{vm.uiState} returns MutableStateFlow(ReportsUiState(isLoading=false,
            salesProfitLast7Days=listOf(SalesProfitDayEntry("01/01",100.0,profit))))
        show{ReportsScreen({},viewModel=vm)}
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("المبيعات مقابل الربح - آخر 7 أيام"))
        ui.mainClock.advanceTimeBy(2000);ui.waitForIdle()
    }
    private fun longestColorColumn(color:Color):Int {
        // Robolectric has no hardware window redraw callback for PixelCopy. Draw the actual
        // decorated view into a native Skia bitmap instead of fabricating a chart image.
        lateinit var bitmap:Bitmap
        ui.runOnIdle {
            val view=activity.window.decorView
            bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
        }
        FileOutputStream(File("/tmp/coverage95-lifecycle-$fixtureName.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        val pixels=bitmap.asImageBitmap().toPixelMap();var best=0
        for(x in 0 until pixels.width){var run=0;for(y in 0 until pixels.height){
            val p=pixels[x,y];val matches=kotlin.math.abs(p.red-color.red)<.03f&&kotlin.math.abs(p.green-color.green)<.03f&&kotlin.math.abs(p.blue-color.blue)<.03f
            run=if(matches)run+1 else 0;best=maxOf(best,run)
        }};bitmap.recycle();return best
    }
    @Test fun salesBarUsesTheSalesLegendColorAndProfitUsesItsOwnColor(){
        chart(30.0);ui.onNodeWithText("المبيعات").assertExists();ui.onNodeWithText("الربح").assertExists()
        val salesHeight=longestColorColumn(Emerald500);val profitHeight=longestColorColumn(Cyan500)
        assertTrue("Sales legend/bar mismatch: sales=$salesHeight profit=$profitHeight",salesHeight>profitHeight*2&&profitHeight>5)
    }
    @Test fun negativeProfitKeepsAVisibleRedLossBar(){chart(-30.0);assertTrue("Negative profit must remain visible",longestColorColumn(DebtRed)>5)}
}
