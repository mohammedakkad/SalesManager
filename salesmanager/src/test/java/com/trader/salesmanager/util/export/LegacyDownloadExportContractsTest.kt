package com.trader.salesmanager.util.export

import android.app.Application
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class LegacyDownloadExportContractsTest {
    private val context=ApplicationProvider.getApplicationContext<Application>()
    private lateinit var directory:File
    private lateinit var destination:File
    @Before fun setup(){
        directory=Files.createTempDirectory("legacy-download").toFile()
        val downloads=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloads.mkdirs();destination=File(downloads,"seller-contract-${directory.name}.xlsx")
    }
    @After fun cleanup(){destination.delete();directory.deleteRecursively()}
    @Test fun legacyDownloadCopiesRealWorkbookAndReplacesAnOlderExport(){
        val source=File(directory,"actual.xlsx")
        XlsxWriter().apply{addRow("أول" to XlsxWriter.CellStyle())}.write(source)
        assertTrue(ExportManager.saveToDownloads(context,source,destination.name));assertArrayEquals(source.readBytes(),destination.readBytes())
        val old=destination.readBytes()
        XlsxWriter().apply{addRow("تقرير جديد ومختلف" to XlsxWriter.CellStyle())}.write(source)
        assertTrue(ExportManager.saveToDownloads(context,source,destination.name));assertArrayEquals(source.readBytes(),destination.readBytes())
        assertFalse(old.contentEquals(destination.readBytes()))
    }
    @Test fun missingLegacySourceReturnsFailureWithoutInventingAFile(){
        assertFalse(ExportManager.saveToDownloads(context,File(directory,"missing.xlsx"),destination.name))
        assertFalse(destination.exists())
    }
}
