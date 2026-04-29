package com.trader.salesmanager.ui.customers.addedit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.ui.theme.*
import com.trader.salesmanager.ui.theme.appColors
import org.koin.androidx.compose.koinViewModel

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

    Column(
        modifier = Modifier
        .fillMaxSize()
        .background(appColors.screenBackground)
        .verticalScroll(rememberScrollState())
    ) {
        // ── Header Gradient ───────────────────────────────────────
        Box(
            modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    if (uiState.isEditMode) listOf(Violet500, Cyan500)
                    else listOf(Emerald700, Emerald500)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                // Avatar دائري
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (uiState.isEditMode) Icons.Rounded.Edit else Icons.Rounded.PersonAdd,
                        null, tint = Color.White, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (uiState.isEditMode) "تعديل بيانات الزبون" else "إضافة زبون جديد",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (uiState.isEditMode) "قم بتعديل البيانات ثم احفظ"
                        else "أدخل اسم الزبون ورقمه",
                        color = Color.White.copy(0.75f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── بطاقة اسم الزبون ─────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Person, null,
                            tint = Emerald500, modifier = Modifier.size(18.dp))
                        Text("الاسم", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall)
                    }

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = {
                            Text("اسم الزبون *")
                        },
                        placeholder = {
                            Text("مثال: أحمد محمد")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.nameError != null && uiState.nameError != "reserved",
                        supportingText = when {
                            uiState.nameError == "reserved" -> null // تُعرض البطاقة بدل الرسالة
                            uiState.nameError != null || uiState.error != null ->
                            {
                                {
                                    Text(uiState.nameError ?: uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                                }
                            } else -> null
                        },
                        trailingIcon = when {
                            uiState.isReservedName -> {
                                {
                                    Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            uiState.name.trim().length >= 2 && !uiState.isReservedName -> {
                                {
                                    Icon(Icons.Rounded.CheckCircleOutline, null, tint = Emerald500)
                                }
                            } else -> null
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = appColors.border
                        )
                    )

                    // ✅ بطاقة الاسم المحجوز — جميلة وإيجابية لا عدائية
                    AnimatedVisibility(
                        visible = uiState.isReservedName,
                        enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ReservedNameCard()
                    }
                }
            }

            // ── بطاقة رقم الهاتف ─────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Phone, null,
                            tint = Cyan500, modifier = Modifier.size(18.dp))
                        Text("رقم الهاتف", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = appColors.surfaceVariant
                        ) {
                            Text("اختياري",
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = appColors.textSubtle)
                        }
                    }

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
                                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Cyan500)
                                uiState.phoneError != null -> Icon(
                                    Icons.Rounded.ErrorOutline, null,
                                    tint = MaterialTheme.colorScheme.error)
                                uiState.phone.length == 10 -> Icon(
                                    Icons.Rounded.CheckCircleOutline, null, tint = Emerald500)
                            }
                        },
                        supportingText = uiState.phoneError?.let {
                            {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = appColors.border
                        )
                    )

                    // شريط تقدم رقم الهاتف
                    if (uiState.phone.isNotEmpty()) {
                        val progress = uiState.phone.length / 10f
                        val barColor = when {
                            uiState.phoneError != null -> DebtRed
                            progress == 1f -> Emerald500
                            else -> Cyan500
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50))
                                .background(appColors.divider)
                            ) {
                                val animProgress by animateFloatAsState(
                                    progress, tween(300), label = "phone_prog"
                                )
                                Box(
                                    Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                                    .clip(RoundedCornerShape(50)).background(barColor)
                                )
                            }
                            Text(
                                "${uiState.phone.length}/10",
                                style = MaterialTheme.typography.labelSmall,
                                color = barColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // ── زر الحفظ ─────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = uiState.canSave && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isEditMode) Violet500 else Emerald500,
                    disabledContainerColor = appColors.divider
                )
            ) {
                AnimatedContent(
                    targetState = uiState.isLoading,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "btn_content"
                ) {
                    loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (uiState.isEditMode) Icons.Rounded.Save else Icons.Rounded.PersonAdd,
                                null, modifier = Modifier.size(20.dp)
                            )
                            Text(
                                if (uiState.isEditMode) "حفظ التعديلات" else "إضافة الزبون",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── بطاقة الاسم المحجوز ───────────────────────────────────────────
@Composable
private fun ReservedNameCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = UnpaidAmber.copy(0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // العنوان
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(UnpaidAmber.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Info, null,
                        tint = UnpaidAmber, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "اسم محجوز للنظام",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UnpaidAmber
                    )
                    Text(
                        "هذا الاسم مخصص للزبون الزائر",
                        style = MaterialTheme.typography.bodySmall,
                        color = UnpaidAmber.copy(0.8f)
                    )
                }
            }

            HorizontalDivider(color = UnpaidAmber.copy(0.2f))

            // شرح الزبون الزائر
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Storefront, null,
                    tint = UnpaidAmber.copy(0.7f),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Text(
                    "\"الزبون الزائر\" هو حساب افتراضي لتسجيل عمليات سريعة بدون تحديد زبون. " +
                    "الرجاء استخدام اسم مختلف لزبونك الجديد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}