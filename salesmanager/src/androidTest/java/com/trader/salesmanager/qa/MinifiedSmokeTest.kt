package com.trader.salesmanager.qa

import android.app.Application
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trader.salesmanager.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * R8 smoke test, run only by the minified lane (-PsellerTestBuildType=staging, package ".test"):
 * the real Application starts from the shrunk/obfuscated APK (Koin graph, Room, WorkManager,
 * Firebase with the offline fixture), MainActivity reaches RESUMED, draws Compose content and
 * stays alive. It touches only public entry points so R8 is free to rename everything else.
 */
@RunWith(AndroidJUnit4::class)
class MinifiedSmokeTest {
    @Test fun realApplicationAndMainActivityStartFromTheMinifiedApk() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assumeTrue("Runs only against the minified staging build", context.packageName.endsWith(".test"))
        assertEquals("com.trader.salesmanager.SalesManagerApp", context.javaClass.name)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            // Let startup work (Koin resolution, DataStore, Room, workers) run before checking.
            Thread.sleep(5_000)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue("Compose content must be attached", content.childCount > 0)
                assertTrue("Content must have been laid out", content.getChildAt(0).height > 0)
            }
        }
    }
}
