package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.trader.salesmanager.util.pdf.PdfCanvas
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native bitmap rendering checks helpers only; never claims to generate a PDF in the JVM. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfCanvasNativeTest {
    @Test fun roundedCardHasFillBorderAndLeavesOutsidePixelsUntouched() {
        val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
        PdfCanvas.roundRect(canvas,10f,10f,90f,90f,Color.BLUE,5f,Color.RED)
        assertEquals(Color.BLUE,bitmap.getPixel(50,50));assertEquals(Color.TRANSPARENT,bitmap.getPixel(1,1))
        assertEquals(Paint.Style.STROKE,PdfCanvas.stroke(Color.RED,2f).style);assertEquals(2f,PdfCanvas.stroke(Color.RED,2f).strokeWidth,0f)
        assertEquals(Color.BLUE,PdfCanvas.fill(Color.BLUE).color);bitmap.recycle()
    }
    @Test fun arabicTextLayoutWrapsAndCellDrawingStaysWithinItsClip() {
        val text="تقرير مبيعات عربي متعدد الأسطر للتحقق من التفاف النص"
        val wide=PdfCanvas.measureHeight(text,400f,18f);val narrow=PdfCanvas.measureHeight(text,80f,18f,bold=true)
        assertTrue(wide>0);assertTrue(narrow>wide);assertTrue(PdfCanvas.measureHeight(text,0f,18f,maxLines=1)>0)
        val bitmap=Bitmap.createBitmap(300,150,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
        assertTrue(PdfCanvas.text(canvas,"اختبار",10f,10f,200f,18f,Color.BLACK)>0)
        PdfCanvas.cellText(canvas,text,100f,50f,60f,30f,18f,Color.BLUE,bold=true,maxLines=2)
        assertEquals(Color.TRANSPARENT,bitmap.getPixel(299,149));assertTrue((0 until 150).any{y->(0 until 300).any{x->Color.alpha(bitmap.getPixel(x,y))>0}})
        bitmap.recycle()
    }
}
