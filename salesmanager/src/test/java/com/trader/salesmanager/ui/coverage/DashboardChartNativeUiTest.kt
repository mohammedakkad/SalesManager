package com.trader.salesmanager.ui.coverage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import com.trader.core.domain.model.DailySales
import com.trader.salesmanager.ui.home.SalesChartCard
import com.trader.salesmanager.ui.theme.Emerald500
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.rules.TestName
import java.io.File
import java.security.MessageDigest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardChartNativeUiTest:AppThemeUiFixture() {
    @get:Rule(order=2) val currentCase=TestName()
    private var captures=0
    private fun day(offset:Int)=Calendar.getInstance().apply{clear();set(2026,10,14,12,0,0);add(Calendar.DAY_OF_MONTH,offset)}.timeInMillis
    private fun description(values:List<Double>):String {
        val names=listOf("السبت","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة")
        return "المبيعات خلال 7 أيام: "+if(values.isEmpty())"لا توجد بيانات" else values.mapIndexed{i,v->"${names[i]}: ${String.format(java.util.Locale.US,"%.2f",v)}"}.joinToString("; ")
    }
    private fun series(values:List<Double>)=values.mapIndexed{i,v->DailySales(day(i),v)}
    private data class CurveSample(val pixels:Int,val hasValley:Boolean)
    private fun greenPixels(description:String,expectValley:Boolean=false):Int {
        // Vico publishes its model from a worker coroutine. Advancing the Compose clock
        // before that publication does not settle its subsequent initial animation.
        ui.waitUntil(5000) {
            ui.mainClock.advanceTimeBy(500)
            val sample=sampleGreenPixels(description, save=false)
            sample.pixels>20&&(!expectValley||sample.hasValley)
        }
        val sample=sampleGreenPixels(description, save=true)
        if(expectValley)assertTrue("The replacement series must render its central zero between two positive endpoints",sample.hasValley)
        return sample.pixels
    }
    private fun sampleGreenPixels(description:String,save:Boolean):CurveSample {
        val b=ui.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        lateinit var bitmap:Bitmap
        ui.runOnIdle{val v=activity.findViewById<View>(android.R.id.content);bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap))}
        val target=Emerald500;var count=0
        val left=mutableListOf<Int>();val middle=mutableListOf<Int>();val right=mutableListOf<Int>()
        for(x in floor(b.left).toInt().coerceAtLeast(0) until ceil(b.right).toInt().coerceAtMost(bitmap.width))
            for(y in floor(b.top).toInt().coerceAtLeast(0) until ceil(b.bottom).toInt().coerceAtMost(bitmap.height)){
                val p=bitmap.getPixel(x,y);val r=(p ushr 16 and 255)/255f;val g=(p ushr 8 and 255)/255f;val blue=(p and 255)/255f
                if(abs(r-target.red)<.03f&&abs(g-target.green)<.03f&&abs(blue-target.blue)<.03f){
                    count++
                    val relativeX=(x-b.left)/b.width
                    when(relativeX){in .15f.. .28f->left+=y;in .46f.. .60f->middle+=y;in .77f.. .90f->right+=y}
                }
            }
        fun median(points:List<Int>)=points.sorted()[points.size/2].toFloat()
        val valley=left.isNotEmpty()&&middle.isNotEmpty()&&right.isNotEmpty()&&
            median(middle)>maxOf(median(left),median(right))+b.height*.15f
        if(save){
        val file=File("build/reports/qa-native-transitions", "${currentCase.methodName}-${captures++}.png")
        requireNotNull(file.parentFile).mkdirs();file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        val digest=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it.toInt() and 255)}
        println("NATIVE_CHART_EVIDENCE file=${file.path} sha256=$digest greenPixels=$count hasValley=$valley description=$description")
        }
        bitmap.recycle();return CurveSample(count,valley)
    }
    @Test fun sevenDayChartExposesEveryActualDayAndAmountAndDrawsItsCurve(){
        val values=listOf(0.0,900.0,1100.0,1000000.0,1300.0,1400.0,1100000.0)
        showApp{SalesChartCard(series(values))};ui.mainClock.advanceTimeBy(6000)
        assertTrue(greenPixels(description(values))>20)
    }
    @Test fun noHistoryIsDescribedWithoutInventingASale(){
        showApp{SalesChartCard(emptyList())}
        ui.onNodeWithContentDescription(description(emptyList())).assertExists()
    }
    @Test fun replacingHistoryUpdatesAccessibleValuesAndRemovesStaleDates(){
        val values=mutableStateOf(listOf(12.5,25.0))
        showApp{SalesChartCard(series(values.value))};ui.onNodeWithContentDescription(description(values.value)).assertExists()
        val old=description(values.value);ui.runOnIdle{values.value=listOf(75.25,0.0,125.5)};ui.mainClock.advanceTimeBy(6000)
        ui.onNodeWithContentDescription(old).assertDoesNotExist()
        assertTrue(greenPixels(description(values.value),expectValley=true)>20)
    }
    @Test fun clearingAnAlreadyDrawnSeriesRemovesItsAccessibleAmounts(){
        val values=mutableStateOf(listOf(100.0,250.0));showApp{SalesChartCard(series(values.value))}
        ui.onNodeWithContentDescription(description(values.value)).assertExists()
        val old=description(values.value);ui.runOnIdle{values.value=emptyList()}
        ui.onNodeWithContentDescription(description(emptyList())).assertExists();ui.onNodeWithContentDescription(old).assertDoesNotExist()
    }
    @Test fun changingThemeRetainsTheSeriesAndItsVisibleCurve(){
        val values=listOf(100.0,200.0,50.0)
        showApp{SalesChartCard(series(values))};ui.mainClock.advanceTimeBy(6000)
        assertTrue(greenPixels(description(values))>20)
        dark(true);ui.mainClock.advanceTimeBy(6000)
        assertTrue(greenPixels(description(values))>20)
    }
}
