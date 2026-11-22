package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.trader.salesmanager.util.pdf.core.PdfPaints
import com.trader.salesmanager.util.pdf.core.PdfTextPainter
import com.trader.salesmanager.util.pdf.design.PdfTextStyle
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Native bitmap rendering checks of the shared PDF drawing helpers; never claims to generate a PDF in the JVM.
 * Migrated from the removed PdfCanvas object to its replacements (PdfPaints, PdfTextPainter) with the same checks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfCanvasNativeTest {
    @Test fun roundedCardHasFillBorderAndLeavesOutsidePixelsUntouched() {
        val paints=PdfPaints()
        val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
        canvas.drawRoundRect(10f,10f,90f,90f,5f,5f,paints.fill(Color.BLUE));canvas.drawRoundRect(10f,10f,90f,90f,5f,5f,paints.stroke(Color.RED))
        assertEquals(Color.BLUE,bitmap.getPixel(50,50));assertEquals(Color.TRANSPARENT,bitmap.getPixel(1,1))
        assertEquals(Paint.Style.STROKE,paints.stroke(Color.RED,2f).style);assertEquals(2f,paints.stroke(Color.RED,2f).strokeWidth,0f)
        assertEquals(Color.BLUE,paints.fill(Color.BLUE).color);assertSame("Paints are cached",paints.fill(Color.BLUE),paints.fill(Color.BLUE))
        bitmap.recycle()
    }
    @Test fun arabicTextLayoutWrapsAndCellDrawingStaysWithinItsClip() {
        val text=PdfTextPainter();val style=PdfTextStyle(18f);val bold=PdfTextStyle(18f,bold=true)
        val sentence="تقرير مبيعات عربي متعدد الأسطر للتحقق من التفاف النص"
        val wide=text.height(sentence,400f,style);val narrow=text.height(sentence,80f,bold)
        assertTrue(wide>0);assertTrue(narrow>wide);assertTrue(text.height(sentence,0f,style,maxLines=1)>0)
        val bitmap=Bitmap.createBitmap(300,150,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
        assertTrue(text.text(canvas,"اختبار",10f,10f,200f,style,Color.BLACK)>0)
        canvas.save();canvas.clipRect(100f,50f,160f,80f);text.text(canvas,sentence,104f,50f,52f,bold,Color.BLUE,maxLines=2);canvas.restore()
        assertEquals(Color.TRANSPARENT,bitmap.getPixel(299,149));assertTrue((0 until 150).any{y->(0 until 300).any{x->Color.alpha(bitmap.getPixel(x,y))>0}})
        bitmap.recycle()
    }
}
