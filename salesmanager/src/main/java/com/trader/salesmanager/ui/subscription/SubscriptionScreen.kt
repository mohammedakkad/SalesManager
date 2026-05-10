package com.trader.salesmanager.ui.subscription

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import com.trader.salesmanager.ui.theme.Cyan400
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.Emerald400
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.UnpaidAmber
import com.trader.salesmanager.ui.theme.Violet500
import com.trader.salesmanager.ui.theme.appColors
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateUp: () -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel()
) {
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = appColors
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateUp()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "ترقية الاشتراك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "رجوع",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.screenBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackState) },
        containerColor = colors.screenBackground
    ) { padding ->
        AnimatedContent(
            targetState = subscriptionState.isPending,
            transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(380)) },
            label = "subRoot",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { isPending ->
            if (isPending) {
                PendingReviewScreen(modifier = Modifier.fillMaxSize())
            } else {
                SubscriptionContent(
                    uiState = uiState,
                    onPlanSelect = viewModel::selectPlan,
                    onPaymentSelect = viewModel::selectPaymentMethod,
                    onReceiptPicked = viewModel::onReceiptPicked,
                    onSubmit = viewModel::submit
                )
            }
        }
    }
}

@Composable
private fun PendingReviewScreen(modifier: Modifier = Modifier) {
    val colors = appColors
    val transition = rememberInfiniteTransition(label = "pendingPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .scale(pulseScale)
                    .background(UnpaidAmber.copy(alpha = pulseAlpha), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(UnpaidAmber.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassBottom,
                    contentDescription = null,
                    tint = UnpaidAmber,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "طلبك قيد المراجعة",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "سيتم مراجعة طلبك والرد عليك خلال 24 ساعة",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardBackground, RoundedCornerShape(20.dp))
                .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "في انتظار الموافقة يمكنك:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                PendingFeatureRow(Icons.Rounded.CheckCircle, "إدارة المبيعات والعملاء بالكامل")
                PendingFeatureRow(Icons.Rounded.CheckCircle, "إدارة الديون بدون قيود")
                PendingFeatureRow(Icons.Rounded.CheckCircle, "استعراض التقارير الأساسية")
            }
        }
    }
}

@Composable
private fun PendingFeatureRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = appColors.textSecondary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Emerald500,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SubscriptionContent(
    modifier: Modifier = Modifier,
    uiState: SubscriptionUiState,
    onPlanSelect: (SubscriptionPlan) -> Unit,
    onPaymentSelect: (SubscriptionPaymentMethod) -> Unit,
    onReceiptPicked: (android.net.Uri) -> Unit,
    onSubmit: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onReceiptPicked) }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PremiumHeroHeader()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SectionTitle(title = "اختر باقتك", subtitle = "يمكنك تغيير باقتك في أي وقت")

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.plans.chunked(2).forEach { rowPlans ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPlans.forEach { plan ->
                            PlanCard(
                                plan = plan,
                                isSelected = uiState.selectedPlan?.id == plan.id,
                                onSelect = { onPlanSelect(plan) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowPlans.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            SectionTitle(title = "طريقة الدفع", subtitle = "حوّل المبلغ ثم ارفع صورة الإيصال")

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.paymentMethods.forEach { method ->
                    PaymentMethodItem(
                        method = method,
                        isSelected = uiState.selectedPaymentMethod?.id == method.id,
                        onSelect = { onPaymentSelect(method) }
                    )
                }
            }

            ReceiptUploadSection(
                receiptBytes = uiState.receiptImageBytes,
                onPickImage = {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )

            GradientSubmitButton(
                enabled = uiState.canSubmit,
                isLoading = uiState.isLoading,
                onClick = onSubmit
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PremiumHeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Emerald700, Cyan500),
                    start = Offset(Float.POSITIVE_INFINITY, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "النسخة المميزة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "ميزات غير محدودة لنمو أعمالك",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroFeatureChip("المخزون")
                Spacer(Modifier.width(8.dp))
                HeroFeatureChip("التقارير")
                Spacer(Modifier.width(8.dp))
                HeroFeatureChip("المرتجعات")
                Spacer(Modifier.width(8.dp))
                HeroFeatureChip("الجرد")
            }
        }
    }
}

@Composable
private fun HeroFeatureChip(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    val colors = appColors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSubtle,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors
    val accentColor = if (plan.isRecommended) Violet500 else Emerald500
    val cardBg by animateColorAsState(if (isSelected) accentColor.copy(0.08f) else colors.cardBackground)
    val plainBorder by animateColorAsState(if (isSelected) accentColor else colors.border)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (plan.isRecommended) Modifier.border(
                    if (isSelected) 2.dp else 1.dp,
                    Brush.linearGradient(listOf(Violet500, Cyan500)),
                    RoundedCornerShape(16.dp)
                )
                else Modifier.border(
                    if (isSelected) 2.dp else 1.dp,
                    plainBorder,
                    RoundedCornerShape(16.dp)
                )
            )
            .background(cardBg)
            .clickable(onClick = onSelect)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            plan.savingBadge?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Violet500,
                                    Cyan500
                                )
                            ), RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                plan.priceLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) accentColor else colors.textPrimary
            )
            Text(
                plan.periodLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSubtle
            )
            Text(
                plan.arabicLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun PlanCardContent(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    accentColor: Color,
    bgColor: Color,
    cornerDp: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp
) {
    val colors = appColors
    val shape = RoundedCornerShape(cornerDp.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (borderColor != null) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .animateContentSize(tween(280)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        plan.savingBadge?.let { badge ->
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(Violet500, Cyan500)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = plan.priceLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) accentColor else colors.textPrimary
        )
        Text(
            text = plan.periodLabel,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSubtle
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = plan.arabicLabel,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun PaymentMethodItem(
    method: SubscriptionPaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colors = appColors
    val clipboard = LocalClipboardManager.current
    var justCopied by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Emerald500 else colors.border,
        animationSpec = tween(240),
        label = "methodBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Emerald500.copy(alpha = 0.07f) else colors.cardBackground,
        animationSpec = tween(240),
        label = "methodBg"
    )

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1600L)
            justCopied = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = method.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = method.accountNumber,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.width(12.dp))

        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(method.accountNumber))
                justCopied = true
            },
            modifier = Modifier.size(36.dp)
        ) {
            AnimatedContent(
                targetState = justCopied,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "copyIcon"
            ) { copied ->
                Icon(
                    imageVector = if (copied) Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
                    contentDescription = if (copied) "تم النسخ" else "نسخ",
                    tint = if (copied) Emerald500 else colors.textSubtle,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        AnimatedContent(
            targetState = isSelected,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "selIndicator"
        ) { selected ->
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.5.dp, colors.border, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ReceiptUploadSection(
    receiptBytes: ByteArray?,
    onPickImage: () -> Unit
) {
    val imageBitmap = remember(receiptBytes) {
        receiptBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            title = "إيصال الدفع",
            subtitle = "ارفع صورة واضحة للإيصال"
        )

        AnimatedContent(
            targetState = imageBitmap,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(320)) },
            label = "receiptArea"
        ) { bitmap ->
            if (bitmap != null) {
                ReceiptPreview(bitmap = bitmap, onReplace = onPickImage)
            } else {
                ReceiptUploadPlaceholder(onPickImage = onPickImage)
            }
        }
    }
}

@Composable
private fun ReceiptUploadPlaceholder(onPickImage: () -> Unit) {
    val colors = appColors
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .drawBehind {
                drawRoundRect(
                    color = colors.border,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onPickImage),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Emerald500.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "اضغط لرفع إيصال الدفع",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = appColors.textPrimary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "PNG · JPG — الحجم الأقصى 150KB",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.textSubtle
            )
        }
    }
}

@Composable
private fun ReceiptPreview(
    bitmap: ImageBitmap,
    onReplace: () -> Unit
) {
    val colors = appColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "إيصال الدفع",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReplace) {
                Text(
                    text = "تغيير الصورة",
                    style = MaterialTheme.typography.labelMedium,
                    color = Cyan400
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "تم رفع الإيصال",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GradientSubmitButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val btnAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.42f,
        animationSpec = tween(240),
        label = "btnAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(btnAlpha)
            .background(
                brush = Brush.horizontalGradient(listOf(Emerald500, Cyan500)),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "btnContent"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "إرسال طلب الاشتراك",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
