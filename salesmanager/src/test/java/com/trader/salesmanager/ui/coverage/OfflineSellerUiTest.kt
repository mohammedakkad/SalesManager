package com.trader.salesmanager.ui.coverage

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.trader.salesmanager.util.export.ExportViewModel
import org.junit.*
import org.junit.rules.TestRule
import org.junit.rules.ExternalResource
import org.junit.runners.model.Statement
import org.koin.core.module.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** No app manifest/providers: only a plain test activity is registered before ActivityScenario starts. */
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
abstract class OfflineSellerUiTest {
    @get:org.junit.Rule(order=-10) val westernDigits=com.trader.salesmanager.testing.WesternDigitsRule()
    @get:Rule(order=-2) val koinLifetime=object:ExternalResource() {
        override fun before(){startKoin { modules(module { viewModel { ExportViewModel() } }) }}
        override fun after(){stopKoin()}
    }
    @get:Rule(order=0) val registerActivity=TestRule { base, _ -> object:Statement() {
        override fun evaluate() {
            val context=ApplicationProvider.getApplicationContext<Application>()
            val pm=shadowOf(context.packageManager)
            val info=pm.addActivityIfNotPresent(ComponentName(context,ComponentActivity::class.java))
            info.applicationInfo=context.applicationInfo;info.exported=true
            info.theme=android.R.style.Theme_Material_Light_NoActionBar
            pm.addOrUpdateActivity(info)
            base.evaluate()
        }
    } }
    @get:Rule(order=1) val ui=createAndroidComposeRule<ComponentActivity>()
    protected val activity:ComponentActivity get()=ui.activity
    private var key=0
    protected fun <T:ViewModel> owned(vm:T):T = vm.also { activity.viewModelStore.put("fixture-${key++}",it) }
    protected fun show(content:@Composable () -> Unit) {
        ui.setContent { MaterialTheme { content() } }
    }
}
