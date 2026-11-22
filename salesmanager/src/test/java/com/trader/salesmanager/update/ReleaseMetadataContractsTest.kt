package com.trader.salesmanager.update

import android.app.Application
import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.net.*
import java.util.concurrent.TimeUnit

/** Metadata transport only: offline connection failures and loopback HTTP, never a qualified/signed APK. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ReleaseMetadataContractsTest {
    private fun release(tag:String="salesmanager-v1.2.6",draft:Boolean=false,pre:Boolean=false)=JSONObject()
        .put("tag_name",tag).put("draft",draft).put("prerelease",pre)
        .put("body","  - إصلاح الفواتير  \n\n* تحسين المخزون\nملاحظة  ")
        .put("assets",JSONArray().put(JSONObject().put("name","notes.txt").put("browser_download_url","https://example.invalid/notes"))
            .put(JSONObject().put("name","salesmanager-release.apk").put("browser_download_url","https://example.invalid/fixture.apk")))
    private class Connection(var body:String,var status:Int=200):HttpURLConnection(URL("https://example.invalid/offline-metadata")) {
        var disconnected=false;var closed=false;var failure:Throwable?=null
        override fun disconnect(){disconnected=true}
        override fun connect(){}
        override fun usingProxy()=false
        override fun getResponseCode():Int {failure?.let{throw it};return status}
        override fun getInputStream():InputStream=object:ByteArrayInputStream(body.toByteArray()) {
            override fun close(){closed=true;super.close()}
        }
    }
    private suspend fun parsed(value:JSONObject)=AppUpdateChecker.check{Connection(value.toString())}
    @Test fun publishedReleaseUsesEncodedVersionExactAssetAndTrimmedChangelog()=runBlocking {
        val c=Connection(release().toString());val info=AppUpdateChecker.check{c}!!
        assertEquals(10206,info.latestVersion);assertEquals("1.2.6",info.versionName)
        assertEquals("https://example.invalid/fixture.apk",info.downloadUrl)
        assertEquals(listOf("إصلاح الفواتير","تحسين المخزون","ملاحظة"),info.changelog)
        assertFalse(info.isForce);assertTrue(c.closed);assertTrue(c.disconnected)
    }
    @Test fun plainVPrefixRemainsCompatible()=runBlocking {assertEquals(20304,parsed(release("v2.3.4"))!!.latestVersion)}
    @Test fun unprefixedVersionRemainsCompatible()=runBlocking {assertEquals(20304,parsed(release("2.3.4"))!!.latestVersion)}
    @Test fun draftNeverBecomesAnUpdateOffer()=runBlocking {assertNull(parsed(release(draft=true)))}
    @Test fun prereleaseNeverBecomesAnUpdateOffer()=runBlocking {assertNull(parsed(release(pre=true)))}
    @Test fun absentAssetsCannotInventAnApk()=runBlocking {assertNull(parsed(release().apply{remove("assets")}))}
    @Test fun emptyAssetsCannotInventAnApk()=runBlocking {assertNull(parsed(release().put("assets",JSONArray())))}
    @Test fun otherApkNameCannotBeInstalledAsSellerRelease()=runBlocking {
        assertNull(parsed(release().put("assets",JSONArray().put(JSONObject().put("name","admin-release.apk").put("browser_download_url","https://example.invalid/wrong.apk")))))
    }
    @Test fun matchingAssetWithoutUrlIsNotAnOffer()=runBlocking {
        assertNull(parsed(release().put("assets",JSONArray().put(JSONObject().put("name","salesmanager-release.apk")))))
    }
    @Test fun blankAssetUrlIsNotAnOffer()=runBlocking {
        assertNull(parsed(release().put("assets",JSONArray().put(JSONObject().put("name","salesmanager-release.apk").put("browser_download_url","")))))
    }
    @Test fun missingVersionTagCannotOfferAnUpdate()=runBlocking {assertNull(parsed(release().apply{remove("tag_name")}))}
    @Test fun malformedJsonIsUnavailableRatherThanAnException()=runBlocking {assertNull(AppUpdateChecker.check{Connection("not json")})}
    @Test fun absentNotesProduceAnEmptyChangelog()=runBlocking {assertEquals(emptyList<String>(),parsed(release().apply{remove("body")} )!!.changelog)}
    @Test fun malformedVersionComponentsAreNotOffers()=runBlocking {
        for(tag in listOf("1.2","1.2.3.4","bad.2.3","1.bad.3","1.2.bad"))assertNull(tag,parsed(release(tag)))
    }
    @Test fun negativeMajorCannotBeOffered()=runBlocking {assertNull(parsed(release("-1.2.3")))}
    @Test fun negativeMinorCannotBeOffered()=runBlocking {assertNull(parsed(release("1.-2.3")))}
    @Test fun negativePatchCannotBeOffered()=runBlocking {assertNull(parsed(release("1.2.-3")))}
    @Test fun overflowedVersionCodeCannotBeOffered()=runBlocking {assertNull(parsed(release("2147483647.0.0")))}
    @Test fun minorBeyondTwoDigitEncodingCannotAliasTheNextMajor()=runBlocking {assertNull(parsed(release("1.100.0")))}
    @Test fun patchBeyondTwoDigitEncodingCannotAliasTheNextMinor()=runBlocking {assertNull(parsed(release("1.2.100")))}
    @Test fun deniedHttpResponseDisconnectsTheConnection()=runBlocking {
        val c=Connection("denied",403);assertNull(AppUpdateChecker.check{c});assertTrue("HTTP failure leaked connection",c.disconnected)
    }
    @Test fun transportFailureDisconnectsTheConnection()=runBlocking {
        val c=Connection("unused").apply{failure=IOException("offline fixture")};assertNull(AppUpdateChecker.check{c});assertTrue(c.disconnected)
    }
    @Test fun cancellationPropagatesAndDisconnectsTheConnection()=runBlocking {
        val c=Connection("unused").apply{failure=CancellationException("offline cancellation")}
        try{AppUpdateChecker.check{c};fail("Cancellation must not become a normal unavailable response")}catch(expected:CancellationException){assertTrue(c.disconnected)}
    }
    @Test fun loopbackRequestUsesGithubHeadersAndParsesTheActualHttpBody()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            server.enqueue(MockResponse().setBody(release().toString()))
            val info=AppUpdateChecker.check{server.url("/releases/latest").toUrl().openConnection() as HttpURLConnection}
            assertEquals(10206,info!!.latestVersion)
            val request=server.takeRequest(5,TimeUnit.SECONDS)!!;assertEquals("GET",request.method);assertEquals("/releases/latest",request.path)
            assertEquals("application/vnd.github+json",request.getHeader("Accept"));assertEquals("2022-11-28",request.getHeader("X-GitHub-Api-Version"))
        }finally{server.shutdown()}
    }
}
