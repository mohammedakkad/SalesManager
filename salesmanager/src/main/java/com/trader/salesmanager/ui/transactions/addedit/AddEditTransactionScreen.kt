package com.trader.salesmanager.ui.transactions.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    preselectedCustomerId: Long?,
    onNavigateUp: () -> Unit,
    onNavigateToInvoiceItems: (customerName: String) -> Unit = {},
    viewModel: AddEditTransactionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val paymentRepo: PaymentMethodRepository = koinInject()

    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods(paymentRepo)
        viewModel.loadTransaction(transactionId)
        viewModel.preselect(preselectedCustomerId)
    }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateUp()
    }

    var customerExpanded by remember {
        mutableStateOf(false)
    }
    var paymentExpanded by remember {
        mutableStateOf(false)
    }

    Column(
        Modifier.fillMaxSize().background(appColors.screenBackground)
        .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Violet500, Cyan500)))
            .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Text(if (uiState.isEditMode) "تعديل العملية" else "إضافة عملية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            }
        }

        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── الزبون ──────────────────────────────────────────────
            ExposedDropdownMenuBox(expanded = customerExpanded, onExpandedChange = {
                customerExpanded = it
            }) {
                OutlinedTextField(
                    value = uiState.selectedCustomer?.name ?: "",
                    onValueChange = {}, readOnly = true, label = {
                        Text("الزبون *")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = uiState.error == "اختر الزبون",
                    leadingIcon = {
                        Icon(Icons.Rounded.Person, null)
                    }
                )
                ExposedDropdownMenu(expanded = customerExpanded, onDismissRequest = {
                    customerExpanded = false
                }) {
                    uiState.customers.forEach {
                        customer ->
                        DropdownMenuItem(text = {
                            Text(customer.name)
                        },
                            onClick = {
                                viewModel.selectCustomer(customer); customerExpanded = false
                            })
                    }
                }
            }

            // ── أصناف الفاتورة ───────────────────────────────────────

            OutlinedButton(
                onClick = {
                    onNavigateToInvoiceItems(uiState.selectedCustomer?.name ?: "زبون")
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (uiState.hasItems) Emerald500 else appColors.divider)
                )
            ) {
                Icon(if (uiState.hasItems) Icons.Rounded.CheckCircle else Icons.Rounded.ShoppingCart,
                    null, tint = if (uiState.hasItems) Emerald500 else appColors.textSubtle,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.hasItems) "أصناف الفاتورة ✓ (${uiState.pendingLines.size} صنف)"
                    else "إضافة أصناف من المخزن (اختياري)",
                    color = if (uiState.hasItems) Emerald500 else appColors.textSubtle
                )
            }
            AnimatedVisibility(uiState.hasItems) {
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald500.copy(0.06f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.pendingLines.forEach {
                            line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "${line.product.product.name} × ${line.displayQtyLabel}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "₪${String.format(java.util.Locale.US, "%.2f", line.totalPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold, color = Emerald500
                                )
                            }
                        }
                    }
                }
            }


            // ── المبلغ ───────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.amount, onValueChange = {
                    v -> viewModel.updateAmount(v.filter {
                        it.isDigit() || it == '.' || it in '٠'..'٩' || it in '۰'..'۹'
                    })
                },
                label = {
                    Text("المبلغ الإجمالي ₪ *")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = uiState.error == "أدخل مبلغ صحيح",
                leadingIcon = {
                    Icon(Icons.Rounded.AttachMoney, null)
                }
            )

            // ── نوع الدفع ────────────────────────────────────────────
            Text("نوع الدفع", style = MaterialTheme.typography.labelLarge,
                color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PaymentType.DEBT to "دين", PaymentType.CASH to "كاش").forEach {
                    (type, label) ->
                    FilterChip(selected = uiState.paymentType == type,
                        onClick = {
                            viewModel.updatePaymentType(type)
                        }, label = {
                            Text(label)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (type == PaymentType.CASH) PaidGreen.copy(0.15f) else Violet500.copy(0.15f),
                            selectedLabelColor = if (type == PaymentType.CASH) PaidGreen else Violet500))
                }
            }

            // ── حالة الدفع ───────────────────────────────────────────
            Text("حالة الدفع", style = MaterialTheme.typography.labelLarge,
                color = appColors.textSecondary, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.isPaid, onClick = {
                    viewModel.updateIsPaid(true)
                },
                    label = {
                        Text("مدفوع")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaidGreen.copy(0.15f), selectedLabelColor = PaidGreen))
                FilterChip(selected = !uiState.isPaid, onClick = {
                    viewModel.updateIsPaid(false)
                },
                    label = {
                        Text("غير مدفوع")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DebtRed.copy(0.15f), selectedLabelColor = DebtRed))
            }

            // ── طريقة الدفع ──────────────────────────────────────────
            ExposedDropdownMenuBox(expanded = paymentExpanded, onExpandedChange = {
                paymentExpanded = it
            }) {
                OutlinedTextField(
                    value = uiState.selectedPaymentMethod?.name ?: "",
                    onValueChange = {}, readOnly = true, label = {
                        Text("طريقة الدفع")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    leadingIcon = {
                        Icon(Icons.Rounded.CreditCard, null)
                    }
                )
                ExposedDropdownMenu(expanded = paymentExpanded, onDismissRequest = {
                    paymentExpanded = false
                }) {
                    uiState.paymentMethods.forEach {
                        method ->
                        DropdownMenuItem(text = {
                            Text(method.name)
                        },
                            onClick = {
                                viewModel.selectPaymentMethod(method); paymentExpanded = false
                            })
                    }
                }
            }

            // ── ملاحظة ───────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.note, onValueChange = viewModel::updateNote,
                label = {
                    Text("ملاحظة (اختياري)")
                },
                modifier = Modifier.fillMaxWidth(), maxLines = 3,
                leadingIcon = {
                    Icon(Icons.Rounded.Notes, null)
                }
            )

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                // 🔴 تعطيل الزر إذا كان التطبيق يحفظ الآن أو إذا تم الحفظ بنجاح
                onClick = viewModel::save,
                enabled = !uiState.isLoading && !uiState.isSaved,
                modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Violet500,
                    disabledContainerColor = Color.Gray // لون رمادي عند التعطيل
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isSaved) "تم الحفظ بنجاح" else "حفظ العملية",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}