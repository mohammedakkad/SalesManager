package com.trader.admin.ui.auth

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalActivity.current as Activity

    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onAuthenticated() }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Navy800, Navy950, Color(0xFF0D0D1A)))),
        contentAlignment = Alignment.Center
    ) {
        // Background glows
        Box(modifier = Modifier.size(300.dp).align(Alignment.TopCenter).offset(y = (-50).dp)
            .clip(CircleShape).background(Indigo500.copy(alpha = 0.08f)))
        Box(modifier = Modifier.size(200.dp).align(Alignment.BottomEnd).offset(x = 50.dp, y = 50.dp)
            .clip(CircleShape).background(Violet500.copy(alpha = 0.08f)))

        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier.size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Indigo500, Violet500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AdminPanelSettings, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("لوحة الإدارة", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                color = Color.White)
            Text("قم بتسجيل الدخول لإدارة البائعين",
                style = MaterialTheme.typography.bodyMedium, color = Slate400, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))

            // Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedContent(state.step, label = "step",
                        transitionSpec = { slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut() }
                    ) { step ->
                        when (step) {
                            AuthStep.PHONE -> PhoneStep(
                                phone = state.phone,
                                onPhoneChange = viewModel::updatePhone,
                                onSend = { viewModel.sendOtp(activity) },
                                isLoading = state.isLoading,
                                error = state.error
                            )
                            AuthStep.OTP -> OtpStep(
                                otp = state.otp,
                                phone = state.phone,
                                onOtpChange = viewModel::updateOtp,
                                onVerify = viewModel::verifyOtp,
                                onBack = { viewModel.updatePhone(state.phone) },
                                isLoading = state.isLoading,
                                error = state.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneStep(phone: String, onPhoneChange: (String) -> Unit, onSend: () -> Unit, isLoading: Boolean, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("رقم الهاتف", style = MaterialTheme.typography.labelLarge, color = Slate300)
        OutlinedTextField(
            value = phone, onValueChange = onPhoneChange,
            placeholder = { Text("+972501234567", color = Slate600) },
            leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = Indigo400) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Button(
            onClick = onSend, enabled = !isLoading && phone.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("إرسال الكود", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun OtpStep(otp: String, phone: String, onOtpChange: (String) -> Unit, onVerify: () -> Unit, onBack: () -> Unit, isLoading: Boolean, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("كود التحقق", style = MaterialTheme.typography.labelLarge, color = Slate300)
        Text("تم إرسال كود إلى $phone", style = MaterialTheme.typography.bodySmall, color = Slate400)
        OutlinedTextField(
            value = otp, onValueChange = onOtpChange,
            placeholder = { Text("000000", color = Slate600) },
            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Indigo400) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Indigo500, unfocusedBorderColor = Slate700,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Button(
            onClick = onVerify, enabled = !isLoading && otp.length == 6,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("تحقق", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("تغيير الرقم", color = Indigo400)
        }
    }
}
