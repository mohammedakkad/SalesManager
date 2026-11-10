package com.trader.salesmanager.util.export

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.nio.file.Files

/** Real XLSX bytes, with deterministic Android resolver failures; no fake or host-generated PDF. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],manifest=Config.NONE,application=Application::class)
class DownloadExportContractsTest {
    private val context=mockk<Context>()
    private val resolver=mockk<ContentResolver>()
    private val uri=Uri.parse("content://media/external/downloads/7")
    private val bytes=ByteArrayOutputStream()
    private lateinit var directory:File
    private lateinit var source:File
    private val inserted=mutableListOf<ContentValues>()
    @Before fun setup(){
        directory=Files.createTempDirectory("download-contract").toFile();source=File(directory,"actual.xlsx")
        XlsxWriter().apply{addRow("صنف & تقرير" to XlsxWriter.CellStyle())}.write(source)
        every{context.contentResolver} returns resolver
        every{resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,any())} answers{inserted+=ContentValues(secondArg<ContentValues>());uri}
        every{resolver.openOutputStream(uri)} returns bytes
        every{resolver.update(uri,any(),null,null)} returns 1
        every{resolver.delete(uri,null,null)} returns 1
    }
    @After fun cleanup(){directory.deleteRecursively()}
    @Test fun successCopiesActualWorkbookBeforePublishingItsMediaStoreEntry(){
        assertTrue(ExportManager.saveToDownloads(context,source,"تقرير.xlsx"));assertArrayEquals(source.readBytes(),bytes.toByteArray())
        val values=slot<ContentValues>();verifyOrder{resolver.openOutputStream(uri);resolver.update(uri,capture(values),null,null)}
        assertEquals(0,values.captured.getAsInteger(MediaStore.Downloads.IS_PENDING))
        assertEquals(1,inserted.single().getAsInteger(MediaStore.Downloads.IS_PENDING))
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",inserted.single().getAsString(MediaStore.Downloads.MIME_TYPE))
        verify(exactly=0){resolver.delete(any(),any(),any())}
    }
    @Test fun deniedInsertCannotBeReportedAsASuccessfulDownload(){
        every{resolver.insert(any<Uri>(),any())} returns null
        assertFalse(ExportManager.saveToDownloads(context,source,"تقرير.xlsx"));verify(exactly=0){resolver.openOutputStream(any())}
    }
    @Test fun absentOutputStreamFailsAndRemovesTheUnpublishedEntry(){
        every{resolver.openOutputStream(uri)} returns null
        assertFalse(ExportManager.saveToDownloads(context,source,"تقرير.xlsx"))
        verify(exactly=1){resolver.delete(uri,null,null)};verify(exactly=0){resolver.update(any(),any(),any(),any())}
    }
    @Test fun failedCopyDoesNotPublishAnIncompleteFileAndCleansUpItsEntry(){
        every{resolver.openOutputStream(uri)} returns object:OutputStream(){override fun write(value:Int){throw IOException("fixture disk full")}}
        assertFalse(ExportManager.saveToDownloads(context,source,"تقرير.xlsx"))
        verify(exactly=0){resolver.update(any(),any(),any(),any())};verify(exactly=1){resolver.delete(uri,null,null)}
    }
    @Test fun failedPublicationCannotBeReportedAsSuccess(){
        every{resolver.update(uri,any(),null,null)} returns 0
        assertFalse(ExportManager.saveToDownloads(context,source,"تقرير.xlsx"));verify(exactly=1){resolver.delete(uri,null,null)}
    }
    @Test fun cleanupFailureCannotEscapeOrChangeTheFailedDownloadResult(){
        every{resolver.openOutputStream(uri)} throws IOException("fixture open failure")
        every{resolver.delete(uri,null,null)} throws SecurityException("fixture cleanup denied")
        assertFalse(ExportManager.saveToDownloads(context,source,"تقرير.xlsx"));verify(exactly=1){resolver.delete(uri,null,null)}
    }
}
