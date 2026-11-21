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
import com.trader.core.domain.model.Product
import com.trader.core.domain.model.ProductUnit
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.MovementType
import com.trader.salesmanager.ui.inventory.detail.*
import kotlinx.coroutines.flow.flowOf
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
import org.koin.core.module.dsl.viewModel
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
            SalesManagerTheme(context=context) {
                val dark=isDarkTheme
                SideEffect{observedDark=dark}
                content()
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
        compose.onNodeWithText(context.getString(R.string.backup_export_title)).performScrollTo().performClick()
        try {
            // Collections retain the node IDs selected on their first fetch. A new
            // dialog owns new nodes, so each poll needs a fresh public query.
            compose.waitUntil(5000){vm.uiState.value.showExportPasswordDialog && compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size==2}
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            val state=vm.uiState.value
            throw AssertionError("Export reopen not ready: requested=${state.showExportPasswordDialog}, exporting=${state.isExporting}, importing=${state.isImporting}, fields=${compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size}",timeout)
        }
        val reopenedFields=compose.onAllNodes(hasSetTextAction())
        reopenedFields.assertCountEquals(2)
        for(index in 0..1){
            reopenedFields[index].assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("")))
        }
        compose.onNodeWithText(context.getString(R.string.backup_export_confirm)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.backup_cancel)).performClick()
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
    @Test fun clearingACountAfterThemeChangeOnlyClearsTheSelectedRoomItem()=runBlocking {
        val session=InventorySession("clear-session","offline-dialog")
        val selected=InventorySessionItem("clear-row",session.id,"p","أرز محفوظ","u","كيس مختار",5.0,4.5)
        val other=InventorySessionItem("untouched-row",session.id,"q","سكر محفوظ","v","علبة باقية",3.0,3.0)
        db.inventoryDao().insertSession(session.toEntity())
        db.inventoryDao().insertSessionItems(listOf(selected.toEntity(),other.toEntity()))
        val repo=InventoryRepositoryImpl(db.inventoryDao(),db.productDao(),unused(StockRepository::class.java),"offline-dialog",db)
        val vm=InventorySessionViewModel(repo,unused(ProductRepository::class.java),"offline-dialog")
        store.put("inventory",vm)
        show{InventorySessionScreen({},viewModel=vm)}
        compose.waitUntil(5000){!vm.uiState.value.isLoading&&vm.sessionItems.value.size==2}
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(selected.unitLabel))
        compose.onNodeWithText(selected.unitLabel).performClick()
        val field=compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
        field.assertTextContains("4.5")
        dark(true);dark(false)
        field.assertTextContains("4.5")
        assertEquals(selected,db.inventoryDao().getSessionItemsOnce(session.id).single{it.id==selected.id}.toDomain())
        compose.onNodeWithText("مسح العدّ").performClick()
        compose.waitUntil(5000){vm.countedItems==1}
        val persisted=db.inventoryDao().getSessionItemsOnce(session.id).map{it.toDomain()}.associateBy{it.id}
        assertEquals(selected.copy(actualQuantity=null),persisted[selected.id])
        assertEquals(other,persisted[other.id])
        assertTrue(db.stockMovementDao().getAllOnce().isEmpty())
    }
    @Test fun encryptedExportAfterThemeChangeProducesAnArchiveDecryptableWithTheOriginalPassword()=runBlocking {
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
    @Test fun encryptedImportPasswordSurvivesThemeAndUnlocksWithoutReplacingRoom()=runBlocking {
        val archived=CustomerEntity(id=7,name="أرشيف للاستعادة",phone="0500000000",createdAt=123)
        db.customerDao().insertCustomer(archived)
        val manager=BackupManager(context,db,"offline-dialog")
        val password="سر الاستعادة الأصلي"
        val file=manager.exportEncryptedBackup(password.toCharArray())
        try {
            val bytes=file.readBytes()
            val current=archived.copy(name="بيانات حالية محفوظة")
            db.customerDao().updateCustomer(current)
            val vm=BackupViewModel(manager,context);store.put("backup",vm)
            show{BackupScreen({},viewModel=vm)}
            // The picker/URI permission path is separate; these are real archive bytes.
            compose.runOnIdle{vm.onImportBytes(bytes)}
            compose.waitUntil(5000){vm.uiState.value.showImportPasswordDialog}
            val field=compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
            field.performTextReplacement(password)
            dark(true)
            context.appDataStore.edit{it[LAST_BACKUP_AT_KEY]=1700000000000}
            dark(false)
            field.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            assertEquals(password.length,field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text.length)
            assertEquals(current,db.customerDao().getCustomerById(7))
            compose.onNodeWithText(context.getString(R.string.backup_import_unlock)).performClick()
            compose.waitUntil(30000){vm.uiState.value.showImportConfirmDialog}
            compose.runOnIdle{assertEquals(archived,vm.uiState.value.pendingImport!!.customers.single{it.id==7L});assertFalse(vm.uiState.value.isImporting)}
            assertEquals(current,db.customerDao().getCustomerById(7))
            compose.onNodeWithText(context.getString(R.string.backup_confirm_no)).performClick()
            compose.runOnIdle{assertNull(vm.uiState.value.pendingImport);assertFalse(vm.uiState.value.showImportConfirmDialog)}
            assertEquals(current,db.customerDao().getCustomerById(7))
        } finally {assertTrue(file.delete())}
        Unit
    }
    @Test fun failedStockAdjustmentRetainsDraftAndExplicitRetryUsesTheEditedQuantity() {
        val unit=ProductUnit("u","p",unitLabel="كيس استعادة",quantityInStock=10.0)
        val product=ProductWithUnits(Product("p",name="صنف استعادة"),listOf(unit))
        val products=object:ProductRepository by unused(ProductRepository::class.java){
            override fun getAllProducts()=flowOf(listOf(product))
        }
        val requests=mutableListOf<Pair<Double,String>>()
        val stock=object:StockRepository by unused(StockRepository::class.java){
            override fun getMovementsForProduct(productId:String,unitId:String)=flowOf(emptyList<com.trader.core.domain.model.StockMovement>())
            override suspend fun manualAdjust(productId:String,unitId:String,quantity:Double,type:MovementType,productName:String,unitLabel:String,note:String){
                assertEquals("p",productId);assertEquals("u",unitId);assertEquals(MovementType.MANUAL_IN,type)
                requests.add(quantity to note)
                if(requests.size==1)throw java.io.IOException("offline fixture write fault")
            }
        }
        val vm=ProductDetailViewModel(products,stock,"p");store.put("detail",vm)
        show{ProductDetailScreen("p",{},{},vm)}
        compose.waitUntil(5000){!vm.uiState.value.isLoading}
        compose.runOnIdle{vm.showAdjust(unit,true)}
        compose.onNode(hasSetTextAction() and hasText("الكمية")).performTextReplacement("2.5")
        compose.onNode(hasSetTextAction() and hasText("ملاحظة (اختياري)")).performTextReplacement("جرد محفوظ")
        compose.onNodeWithText("تطبيق").performClick()
        compose.onNodeWithText("تعذر تعديل المخزون. حاول مرة أخرى.").assertExists()
        dark(true);dark(false)
        compose.onNodeWithText("تطبيق").assertIsEnabled()
        compose.onNode(hasSetTextAction() and hasText("الكمية")).assertTextContains("2.5")
        assertEquals(listOf(2.5 to "جرد محفوظ"),requests)
        compose.onNode(hasSetTextAction() and hasText("الكمية")).performTextReplacement("3.5")
        compose.onNodeWithText("تعذر تعديل المخزون. حاول مرة أخرى.").assertDoesNotExist()
        compose.onNodeWithText("تطبيق").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        assertEquals(listOf(2.5 to "جرد محفوظ",3.5 to "جرد محفوظ"),requests)
        compose.runOnIdle{assertFalse(vm.uiState.value.isSaving)}
    }
    @Test fun actualAndroidEncryptedArchiveRejectsWrongPasswordsAndModifiedCiphertext() {
        val output=java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use{zip->
            zip.putNextEntry(java.util.zip.ZipEntry("fixture.txt"))
            zip.write("اختبار توثيق النسخة".toByteArray(Charsets.UTF_8));zip.closeEntry()
        }
        val plaintext=output.toByteArray();val password="كلمة مرور 🌿".toCharArray()
        val bytes=BackupCrypto.encryptZip(plaintext,password,BACKUP_FORMAT_VERSION,123)
        assertArrayEquals(plaintext,BackupCrypto.decryptToZip(bytes,password))
        assertThrows(BackupException.WrongPasswordOrCorrupted::class.java){BackupCrypto.decryptToZip(bytes,"wrong fixture".toCharArray())}
        val changed=bytes.copyOf();changed[changed.lastIndex]=(changed.last().toInt() xor 1).toByte()
        assertThrows(BackupException.WrongPasswordOrCorrupted::class.java){BackupCrypto.decryptToZip(changed,password)}
        if(android.os.Build.VERSION.SDK_INT>=26){
            val header=BackupCrypto.parseHeader(bytes)
            val spec=javax.crypto.spec.PBEKeySpec(password,header.salt,100000,256)
            val key=try{javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}finally{spec.clearPassword()}
            try{
                val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE,javax.crypto.spec.SecretKeySpec(key,"AES"),javax.crypto.spec.GCMParameterSpec(128,header.iv))
                assertArrayEquals(plaintext,cipher.doFinal(bytes.copyOfRange(61,bytes.size)))
            }finally{key.fill(0)}
        }
    }
    /** These paths must not start stock adjustment, product discovery or network work. */
    private fun <T:Any> unused(type:Class<T>):T = requireNotNull(type.cast(Proxy.newProxyInstance(type.classLoader,arrayOf(type)){_,method,_->
        throw AssertionError("Unexpected collaborator call: ${type.simpleName}.${method.name}")
    }))
}
