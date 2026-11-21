package com.trader.core.activation

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.database.*
import com.trader.core.data.local.appDataStore
import com.trader.core.data.remote.*
import com.trader.core.data.repository.ActivationRepositoryImpl
import com.trader.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ActivationStatusLifecycleTest {
    private val app:Application=ApplicationProvider.getApplicationContext()
    private val merchant=stringPreferencesKey("merchant_code");private val active=booleanPreferencesKey("is_activated")
    private val prefs=MutableStateFlow<Preferences>(emptyPreferences())
    private val store=object:DataStore<Preferences>{
        override val data:Flow<Preferences> = prefs
        override suspend fun updateData(transform:suspend(Preferences)->Preferences)=transform(prefs.value).also{prefs.value=it}
    }
    private val sync=mockk<FirebaseSyncService>(relaxed=true)
    private val db=mockk<FirebaseDatabase>();private val refs=mutableMapOf<String,DatabaseReference>();private val listeners=mutableMapOf<String,ValueEventListener>()
    private fun ref(path:String):DatabaseReference=refs.getOrPut(path){mockk<DatabaseReference>().also{r->
        every{r.child(any())} answers{ref(listOf(path,firstArg<String>()).filter{it.isNotEmpty()}.joinToString("/"))}
        every{r.addValueEventListener(any())} answers{listeners[path]=firstArg();firstArg()}
        every{r.removeEventListener(any<ValueEventListener>())} just Runs
    }}
    private lateinit var repo:ActivationRepositoryImpl
    @Before fun setup(){
        mockkStatic("com.trader.core.data.local.DataStoreProviderKt");every{any<Context>().appDataStore} returns store
        mockkStatic(FirebaseDatabase::class);every{FirebaseDatabase.getInstance()} returns db;every{db.reference} answers{ref("")}
        repo=ActivationRepositoryImpl(app,sync,mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true))
    }
    @After fun cleanup(){unmockkStatic(FirebaseDatabase::class);unmockkStatic("com.trader.core.data.local.DataStoreProviderKt")}
    private fun session(code:String,activated:Boolean=true){prefs.value=preferencesOf(merchant to code,active to activated)}
    private fun emit(path:String,value:String?,exists:Boolean=true){
        val snap=mockk<DataSnapshot>();every{snap.exists()} returns exists;every{snap.child("status")} returns snap;every{snap.getValue(String::class.java)} returns value
        listeners.getValue(path).onDataChange(snap)
    }
    @Test fun blankSessionEmitsNoStatusAndDoesNotSubscribeToAnActivationPath()=runTest {
        val got=mutableListOf<MerchantStatus?>();repo.observeMerchantStatus().onEach{got+=it}.launchIn(backgroundScope);runCurrent()
        assertEquals(listOf<MerchantStatus?>(null),got);assertTrue(listeners.isEmpty())
    }
    @Test fun statusChangesMapKnownCaseVariantsAndUnknownOrMissingValuesToNull()=runTest {
        session("a");val got=mutableListOf<MerchantStatus?>();repo.observeMerchantStatus().onEach{got+=it}.launchIn(backgroundScope);runCurrent()
        for(value in listOf("active","DISABLED","expired","future",null)){emit("activation_codes/a",value);runCurrent()}
        emit("activation_codes/a",null,false);runCurrent()
        assertEquals(listOf(MerchantStatus.ACTIVE,MerchantStatus.DISABLED,MerchantStatus.EXPIRED,null,null,null),got)
    }
    @Test fun switchingMerchantDetachesOldListenerAndLogoutDetachesTheCurrentOne()=runTest {
        session("a");val got=mutableListOf<MerchantStatus?>();val task=repo.observeMerchantStatus().onEach{got+=it}.launchIn(backgroundScope);runCurrent()
        val old=listeners.getValue("activation_codes/a");emit("activation_codes/a","ACTIVE");runCurrent()
        session("b");runCurrent();verify(exactly=1){ref("activation_codes/a").removeEventListener(old)}
        // A callback already queued by the old SDK listener cannot reach the new flow.
        emit("activation_codes/a","DISABLED");emit("activation_codes/b","ACTIVE");runCurrent()
        assertEquals(listOf(MerchantStatus.ACTIVE,MerchantStatus.ACTIVE),got)
        val current=listeners.getValue("activation_codes/b");session("");runCurrent()
        assertNull(got.last());verify(exactly=1){ref("activation_codes/b").removeEventListener(current)}
        task.cancelAndJoin()
    }
    @Test fun collectorCancellationAlwaysRemovesTheRegisteredListener()=runTest {
        session("a");val task=repo.observeMerchantStatus().launchIn(backgroundScope);runCurrent();val listener=listeners.getValue("activation_codes/a")
        task.cancelAndJoin();verify(exactly=1){ref("activation_codes/a").removeEventListener(listener)}
    }
    @Test fun startupValidationKeepsOfflineSeparateFromDisabledExpiredAndDeleted()=runBlocking {
        session("a")
        for((wire,expected) in listOf(ValidationResult.Active to StartupStatus.ACTIVE,ValidationResult.Disabled to StartupStatus.DISABLED,ValidationResult.Expired to StartupStatus.EXPIRED,ValidationResult.NotFound to StartupStatus.DELETED,ValidationResult.NetworkError to StartupStatus.OFFLINE)){
            coEvery{sync.validateCodeDetailed("a")} returns wire;assertEquals(expected,repo.verifyStatusOnStartup())
        }
        assertEquals("a",repo.getMerchantCode());assertTrue(repo.isActivated())
    }
    @Test fun inactiveAndBlankStartupSessionsNeverValidateRemotely()=runBlocking {
        session("a",false);assertEquals(StartupStatus.NOT_ACTIVATED,repo.verifyStatusOnStartup())
        session("   ");assertEquals(StartupStatus.NOT_ACTIVATED,repo.verifyStatusOnStartup());coVerify(exactly=0){sync.validateCodeDetailed(any())}
    }
}
