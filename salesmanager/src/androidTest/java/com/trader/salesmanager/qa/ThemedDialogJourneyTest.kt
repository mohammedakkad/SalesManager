package com.trader.salesmanager.qa

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trader.core.data.local.appDataStore
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.toEntity
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

    @Before fun prepare()=runBlocking {
        context.appDataStore.edit{it[DARK_MODE_KEY]=false}
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        startKoin{modules(module{viewModel{ExportViewModel()}})}
        Unit
    }
    @After fun cleanup() {
        compose.runOnUiThread{store.clear()}
        compose.activityRule.scenario.close()
        db.close()
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
        for(index in 0..1)fields[index].assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,AnnotatedString("fixture secret")))
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
    /** These paths must not start stock adjustment, product discovery or network work. */
    private fun <T:Any> unused(type:Class<T>):T = requireNotNull(type.cast(Proxy.newProxyInstance(type.classLoader,arrayOf(type)){_,method,_->
        throw AssertionError("Unexpected collaborator call: ${type.simpleName}.${method.name}")
    }))
}
