package com.trader.salesmanager.qa

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.CustomerRepository
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerScreen
import com.trader.salesmanager.ui.customers.addedit.AddEditCustomerViewModel
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerJourneyTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val store=ViewModelStore()
    private val repository=FixtureCustomers()
    @After fun cleanup() { compose.runOnUiThread { store.clear() } }
    private fun show(id:Long?,onNavigate:()->Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme=darkColorScheme()) {
                    val vm=androidx.compose.runtime.remember { AddEditCustomerViewModel(repository).also { store.put("customer",it) } }
                    AddEditCustomerScreen(id,onNavigate,vm)
                }
            }
        }
    }
    @Test fun createArabicCustomerValidatesFormSavesOnceAndNavigatesBack() {
        var navigated=0
        show(null) { navigated++ }
        compose.onNodeWithText("إضافة الزبون").assertIsNotEnabled()
        compose.onNodeWithText("اسم الزبون *").performTextInput("أحمد محمد")
        compose.onNodeWithText("إضافة الزبون").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(5000) { repository.rows.value.size==1 && navigated==1 }
        compose.runOnIdle { assertEquals("أحمد محمد",repository.rows.value.single().name);assertEquals(1,navigated) }
    }
    @Test fun editCustomerPreservesIdAndDoesNotCreateDuplicate() {
        repository.rows.value=listOf(Customer(id=7,name="عميل قديم"))
        var navigated=0
        show(7) { navigated++ }
        compose.onNodeWithText("اسم الزبون *").performTextClearance()
        compose.onNodeWithText("اسم الزبون *").performTextInput("عميل جديد")
        compose.onNodeWithText("حفظ التعديلات").performScrollTo().performClick()
        compose.waitUntil(5000) { repository.rows.value.single().name=="عميل جديد" && navigated==1 }
        compose.runOnIdle { assertEquals(7L,repository.rows.value.single().id);assertEquals(1,repository.rows.value.size) }
    }
}

private class FixtureCustomers:CustomerRepository {
    val rows=MutableStateFlow<List<Customer>>(emptyList())
    override fun getAllCustomers():Flow<List<Customer>> = rows
    override fun searchCustomers(query:String)=rows.map { list -> list.filter { it.name.contains(query) } }
    override suspend fun getCustomerById(id:Long)=rows.value.find { it.id==id }
    override suspend fun insertCustomer(customer:Customer):Long { val id=(rows.value.maxOfOrNull { it.id }?:0)+1;rows.value=rows.value+customer.copy(id=id);return id }
    override suspend fun updateCustomer(customer:Customer) { rows.value=rows.value.map { if(it.id==customer.id) customer else it } }
    override suspend fun deleteCustomer(customer:Customer) { rows.value=rows.value.filterNot { it.id==customer.id } }
    override suspend fun getPhoneConflict(phone:String,excludeId:Long)=rows.value.find { it.phone==phone && it.id!=excludeId }?.name
    override suspend fun getTransactionCount(customerId:Long)=0
}
