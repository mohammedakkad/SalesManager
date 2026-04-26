package com.trader.salesmanager.ui.customers.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    customerId: Long?,
    onNavigateUp: () -> Unit,
    viewModel: AddEditCustomerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateUp()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "تعديل زبون" else "إضافة زبون")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) {
        padding ->
        Column(
            modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = {
                    Text("اسم الزبون *")
                },
                isError = uiState.nameError != null || uiState.error != null,
                supportingText = (uiState.nameError ?: uiState.error)?.let {
                    {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::updatePhone,
                label = {
                    Text("رقم الهاتف")
                },
                placeholder = {
                    Text("05XXXXXXXX")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.phoneError != null,
                trailingIcon = {
                    when {
                        uiState.phoneChecking -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                        )
                        uiState.phoneError != null -> Icon(
                            Icons.Rounded.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        uiState.phone.length == 10 && !uiState.phoneChecking -> Icon(
                            Icons.Rounded.CheckCircleOutline, null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                supportingText = uiState.phoneError?.let {
                    {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (uiState.isLoading)
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else
                    Text(if (uiState.isEditMode) "حفظ التعديلات" else "إضافة")
            }
        }
    }
}