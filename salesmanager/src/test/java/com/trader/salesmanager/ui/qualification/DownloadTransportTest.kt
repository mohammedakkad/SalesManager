package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import com.trader.salesmanager.update.AppUpdateDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Loopback HTTP transport fixtures; these bytes are not claimed to be a valid or signed APK. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class DownloadTransportTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var server:MockWebServer
    private val bytes=ByteArray(20000){(it%251).toByte()}
    private val target get()=File(context.cacheDir,"update/salesmanager-update.apk")
    @Before fun start(){
        target.delete();server=MockWebServer();server.start()
    }
    @After fun stop(){server.shutdown();target.delete()}
    private fun url(path:String)=server.url("/$path").toString()
    private fun response()=MockResponse().setBody(Buffer().write(bytes))
    @Test fun completeDownloadPreservesBytesAndMonotonicProgress()=runBlocking {
        server.enqueue(response())
        val progress=mutableListOf<Int>();val file=AppUpdateDownloader.downloadApk(context,url("complete")){progress+=it}
        assertNotNull(file);assertArrayEquals(bytes,file!!.readBytes());assertEquals(100,progress.last());assertTrue(progress.zipWithNext().all{it.first<=it.second})
    }
    @Test fun unknownContentLengthIsSupportedAndHttpFailureDoesNotSucceed()=runBlocking {
        server.enqueue(MockResponse().setChunkedBody(Buffer().write(bytes),8192))
        val progress=mutableListOf<Int>();val file=AppUpdateDownloader.downloadApk(context,url("chunked")){progress+=it}
        assertNotNull(file);assertArrayEquals(bytes,file!!.readBytes());assertTrue(0 in progress);assertEquals(100,progress.last())
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(AppUpdateDownloader.downloadApk(context,url("error")){})
    }
    @Test fun truncatedDownloadIsRejectedAndPartialArtifactRemoved()=runBlocking {
        server.enqueue(response().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        assertNull("Truncated HTTP content cannot be offered for installation",AppUpdateDownloader.downloadApk(context,url("truncated")){})
        assertFalse("Incomplete download must be removed",target.exists())
    }
    @Test fun cancelledDownloadPropagatesCancellationAndRemovesPartialArtifact()=runBlocking {
        server.enqueue(response())
        try{AppUpdateDownloader.downloadApk(context,url("complete")){throw CancellationException("fixture cancellation")};fail("Cancellation converted to a normal result")}
        catch(expected:CancellationException){}
        assertFalse("Cancelled download must not leave an installable artifact",target.exists())
    }
}
