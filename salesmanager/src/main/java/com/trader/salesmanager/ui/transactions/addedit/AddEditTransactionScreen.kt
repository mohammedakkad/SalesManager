package com.trader.salesmanager.ui.transactions.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.PaymentMethod
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.trader.core.domain.repository.PaymentMethodRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    preselectedCustomerId: Long?,
    onNavigateUp: () -> Unit,
    viewModel: AddEditTransactionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val paymentRepo: PaymentMethodRepository = koinInject()

    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods(paymentRepo)
        viewModel.loadTransaction(transactionId)
        viewModel.preselect(preselectedCustomerId)
    }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateUp() }

    var customerExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "تعديل العملية" else "إضافة عملية", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Customer Dropdown ──
            ExposedDropdownMenuBox(expanded = customerExpanded, onExpandedChange = { customerExpanded = it }) {
                OutlinedTextField(
                    value = uiState.selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("الزبون") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = uiState.error == "اختر الزبون"
                )
                ExposedDropdownMenu(expanded = customerExpanded, onDismissRequest = { customerExpanded = false }) {
                    uiState.customers.forEach { customer ->
                        DropdownMenuItem(text = { Text(customer.name) }, onClick = { viewModel.selectCustomer(customer); customerExpanded = false })
                    }
                }
            }

            // ── Amount ──
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::updateAmount,
                label = { Text("المبلغ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.error == "أدخل مبلغ صحيح"
            )

            // ── Payment Status ──
            Text("حالة الدفع", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = uiState.isPaid,  onClick = { viewModel.updateIsPaid(true)  }, label = { Text("مدفوع") })
                FilterChip(selected = !uiState.isPaid, onClick = { viewModel.updateIsPaid(false) }, label = { Text("غير مدفوع") })
            }

            // ── Payment Method Dropdown ──
            ExposedDropdownMenuBox(expanded = paymentExpanded, onExpandedChange = { paymentExpanded = it }) {
                OutlinedTextField(
                    value = uiState.selectedPaymentMethod?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("طريقة الدفع") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                    uiState.paymentMethods.forEach { method ->
                        DropdownMenuItem(text = { Text(method.name) }, onClick = { viewModel.selectPaymentMethod(method); paymentExpanded = false })
                    }
                }
            }

            // ── Note ──
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                label = { Text("ملاحظة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("حفظ العملية", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}