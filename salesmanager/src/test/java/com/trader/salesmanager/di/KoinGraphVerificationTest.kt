package com.trader.salesmanager.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.test.verify.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Two checks of the production seller graph (salesManagerModule, as SalesManagerApp starts it):
 * a static Koin `verify()` of every constructor dependency, and a runtime pass that actually
 * resolves every definition against an offline FirebaseApp (no network, no real project).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class KoinGraphVerificationTest {
    private lateinit var app: KoinApplication

    @Before fun firebase() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:0000000000000000").setApiKey("offline-test-fixture-not-a-credential")
                .setProjectId("seller-qa-offline").setDatabaseUrl("https://seller-qa-offline.firebaseio.com").build())
        }
    }

    /** Resolved view models start work in init; clearing cancels their scopes before the next test class runs. */
    private val store = androidx.lifecycle.ViewModelStore()

    @After fun close() {
        store.clear()
        if (::app.isInitialized) app.close()
        val context = ApplicationProvider.getApplicationContext<Application>()
        FirebaseApp.getApps(context).forEach { runCatching { com.google.firebase.firestore.FirebaseFirestore.getInstance(it).terminate() }; it.delete() }
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test fun everyConstructorDependencyIsDeclared() {
        salesManagerModule.verify(extraTypes = listOf(
            Context::class, Application::class, SavedStateHandle::class, WorkerParameters::class,
            // Values passed explicitly in the module (constants, or parametersOf at the call site).
            String::class, Long::class, Unit::class, Boolean::class, Int::class, Double::class,
            kotlinx.coroutines.flow.Flow::class, kotlinx.coroutines.CoroutineScope::class, kotlinx.coroutines.CoroutineDispatcher::class,
            Function0::class, Function1::class, Function2::class, kotlin.coroutines.SuspendFunction0::class, kotlin.coroutines.SuspendFunction1::class
        ))
    }

    @OptIn(KoinInternalApi::class)
    @Test fun everyDefinitionResolvesAtRuntime() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        app = koinApplication { androidContext(context); modules(salesManagerModule) }
        val definitions = app.koin.instanceRegistry.instances.values.map { it.beanDefinition }.distinct()
        val failures = definitions.mapNotNull { def ->
            val params = PARAMETERS[def.primaryType.simpleName] ?: parametersOf()
            runCatching { app.koin.get<Any>(def.primaryType, def.qualifier) { params }.also { keep(def.primaryType.simpleName, it) } }
                .exceptionOrNull()?.let { "${def.primaryType.simpleName}: ${generateSequence(it) { e -> e.cause }.last()}" }
        }
        println("Resolved ${definitions.size - failures.size}/${definitions.size} definitions")
        assertTrue("Unresolvable definitions:\n${failures.joinToString("\n")}", failures.isEmpty())
        assertTrue("Expected the full graph, got ${definitions.size}", definitions.size > 50)
    }

    @Suppress("RestrictedApi")
    private fun keep(name: String?, instance: Any) {
        if (instance is androidx.lifecycle.ViewModel) store.put("verify-$name", instance)
    }

    private companion object {
        /** Navigation arguments the screens pass with parametersOf(...). */
        val PARAMETERS = mapOf(
            "CustomerDetailsViewModel" to parametersOf(7L),
            "TransactionDetailsViewModel" to parametersOf(41L),
            "DayTransactionsViewModel" to parametersOf(1767225600000L),
            "ProductDetailViewModel" to parametersOf("p1"),
            "ReturnViewModel" to parametersOf(41L, Unit),
            // koinViewModel() supplies this from the owner's CreationExtras on a real screen.
            "AddEditTransactionViewModel" to parametersOf(SavedStateHandle())
        )
    }
}
