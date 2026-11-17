package com.trader.salesmanager.qa

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import androidx.core.content.IntentCompat
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trader.core.data.local.appDataStore
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.local.entity.CustomerEntity
import java.security.MessageDigest
import com.trader.core.data.repository.InventoryRepositoryImpl
import com.trader.core.domain.model.InventorySession
import com.trader.core.domain.model.InventorySessionItem
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.inventory.session.*
import com.trader.salesmanager.ui.settings.backup.*
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.util.export.ExportViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.lang.reflect.Proxy

/** Actual Android dialogs and app theme; only isolated local Room/DataStore data is opened. */
@RunWith(AndroidJUnit4::class)
class ThemedDialogJourneyTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val store=ViewModelStore()
    private lateinit var db:AppDatabase
    @Volatile private var observedDark:Boolean?=null
    private var intentsInitialized=false

    @Before fun prepare()=runBlocking {
        context.appDataStore.edit{it[DARK_MODE_KEY]=false}
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        startKoin{modules(module{viewModel{ExportViewModel()}})}
        Intents.init();intentsInitialized=true
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_CANCELED,null))
        Unit
    }
    @After fun cleanup() {
        compose.runOnUiThread{store.clear()}
        compose.activityRule.scenario.close()
        db.close()
        if(intentsInitialized)Intents.release()
        runBlocking{context.appDataStore.edit{it[DARK_MODE_KEY]=false}}
        stopKoin()
    }
    private fun show(content:@Composable ()->Unit) {
        compose.setContent {
            org.koin.compose.KoinContext {
                SalesManagerTheme(context=context) {
                    val dark=isDarkTheme
                    SideEffect{observedDark=dark}
                    content()
                }
            }
        }
        compose.waitUntil(5000){observedDark==false}
    }
    private fun dark(value:Boolean) {
        runBlocking{context.appDataStore.edit{it[DARK_MODE_KEY]=value}}
        compose.waitUntil(5000){observedDark==value}
    }
    @Test fun exportDialogKeepsBothPasswordsAcrossThemeChangeAndCancelDoesNotExport() {
        val vm=BackupViewModel(BackupManager(context,db,"offline-dialog"),context)
        store.put("backup",vm)
        val originalFiles=context.cacheDir.listFiles()?.map{it.name}?.toSet().orEmpty()
        show{BackupScreen({},viewModel=vm)}
        compose.onNodeWithText(context.getString(R.string.backup_export_title)).performScrollTo().performClick()
        val fields=compose.onAllNodes(hasSetTextAction())
        fields.assertCountEquals(2)
        fields[0].performTextReplacement("fixture secret")
        fields[1].performTextReplacement("fixture secret")
        dark(true)
        for(index in 0..1){
            fields[index].assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            val displayed=fields[index].fetchSemanticsNode().config[SemanticsProperties.EditableText].text
            assertEquals("Password draft length changed after theme", "fixture secret".length, displayed.length)
        }
        compose.onNodeWithText(context.getString(R.string.backup_export_confirm)).assertIsEnabled()
        compose.runOnIdle{assertFalse(vm.uiState.value.isExporting);assertNull(vm.uiState.value.shareFilePath)}
        compose.onNodeWithText(context.getString(R.string.backup_cancel)).performClick()
        dark(false)
        fields.assertCountEquals(0)
        compose.runOnIdle{assertFalse(vm.uiState.value.showExportPasswordDialog);assertFalse(vm.uiState.value.isExporting)}
        assertEquals(originalFiles.filter{it.startsWith("sales_backup_")}.toSet(),context.cacheDir.listFiles()?.map{it.name}?.filter{it.startsWith("sales_backup_")}?.toSet().orEmpty())
    }
    @Test fun inventoryCountDraftSurvivesThemeRoundTripAndOnlyConfirmationWritesRoom()=runBlocking {
        val session=InventorySession("dialog-session","offline-dialog")
        val item=InventorySessionItem("dialog-row",session.id,"p","قهوة جرد","u","حبة جرد",5.0)
        db.inventoryDao().insertSession(session.toEntity())
        db.inventoryDao().insertSessionItems(listOf(item.toEntity()))
        val repo=InventoryRepositoryImpl(db.inventoryDao(),db.productDao(),unused(StockRepository::class.java),"offline-dialog",db)
        val vm=InventorySessionViewModel(repo,unused(ProductRepository::class.java),"offline-dialog")
        store.put("inventory",vm)
        show{InventorySessionScreen({},viewModel=vm)}
        compose.waitUntil(5000){!vm.uiState.value.isLoading&&vm.sessionItems.value.any{it.id==item.id}}
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("حبة جرد"))
        compose.onNodeWithText("حبة جرد").performClick()
        val field=compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
        field.performTextReplacement("7.5")
        dark(true);dark(false)
        field.assertTextContains("7.5")
        assertNull(db.inventoryDao().getSessionItemsOnce(session.id).single().actualQuantity)
        compose.onNodeWithText("تأكيد").performClick()
        compose.waitUntil(5000){vm.countedItems==1}
        assertEquals(item.copy(actualQuantity=7.5),db.inventoryDao().getSessionItemsOnce(session.id).single().toDomain())
        assertTrue(db.stockMovementDao().getAllOnce().isEmpty())
    }
    @Test fun encryptedExportAfterThemeChangeProducesAnArchiveDecryptableWithTheOriginalPassword()=runBlocking {
        javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val customer=CustomerEntity(id=7,name="نسخة واجهة",phone="0500000000",createdAt=123)
        db.customerDao().insertCustomer(customer)
        val manager=BackupManager(context,db,"offline-dialog")
        val vm=BackupViewModel(manager,context)
        store.put("backup",vm)
        val existing=context.cacheDir.listFiles()?.map{it.name}?.toSet().orEmpty()
        show{BackupScreen({},viewModel=vm)}
        compose.onNodeWithText(context.getString(R.string.backup_export_title)).performScrollTo().performClick()
        val password="original fixture phrase"
        val fields=compose.onAllNodes(hasSetTextAction())
        fields.assertCountEquals(2)
        fields[0].performTextReplacement(password);fields[1].performTextReplacement(password)
        dark(true);dark(false)
        compose.onNodeWithText(context.getString(R.string.backup_export_confirm)).assertIsEnabled().performClick()
        compose.waitUntil(30000){Intents.getIntents().any{it.action==Intent.ACTION_CHOOSER}}
        intended(hasAction(Intent.ACTION_CHOOSER))
        val chooser=Intents.getIntents().single{it.action==Intent.ACTION_CHOOSER}
        val send=requireNotNull(IntentCompat.getParcelableExtra(chooser,Intent.EXTRA_INTENT,Intent::class.java))
        assertEquals(Intent.ACTION_SEND,send.action);assertEquals("application/octet-stream",send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
        val uri=requireNotNull(IntentCompat.getParcelableExtra(send,Intent.EXTRA_STREAM,android.net.Uri::class.java))
        assertEquals("content",uri.scheme);assertEquals("${context.packageName}.provider",uri.authority)
        val bytes=requireNotNull(context.contentResolver.openInputStream(uri)).use{it.readBytes()}
        val payload=manager.parseBackupFromBytes(bytes,password.toCharArray())
        assertEquals(customer,payload.customers.single{it.id==7L})
        assertEquals(customer,db.customerDao().getCustomerById(7))
        assertTrue(payload.exportedAt>0)
        compose.runOnIdle{assertFalse(vm.uiState.value.isExporting);assertNull(vm.uiState.value.shareFilePath)}
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
        println("ACTUAL_ANDROID_ENCRYPTED_BACKUP bytes=${bytes.size} sha256=$digest customerId=7")
        context.cacheDir.listFiles()?.filter{it.name.startsWith("sales_backup_")&&it.name !in existing}?.forEach{assertTrue(it.delete())}
        Unit
    }
    /** These paths must not start stock adjustment, product discovery or network work. */
    private fun <T:Any> unused(type:Class<T>):T = requireNotNull(type.cast(Proxy.newProxyInstance(type.classLoader,arrayOf(type)){_,method,_->
        throw AssertionError("Unexpected collaborator call: ${type.simpleName}.${method.name}")
    }))
}
