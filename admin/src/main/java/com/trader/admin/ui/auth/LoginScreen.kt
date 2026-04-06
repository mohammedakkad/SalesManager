package com.trader.admin.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trader.admin.R
import com.trader.admin.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Navy800, Navy950, Color(0xFF0D0D1A)))),
        contentAlignment = Alignment.Center
    ) {
        // خلفية زخرفية
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-60).dp)
                .clip(CircleShape)
                .background(Indigo500.copy(alpha = 0.07f))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Violet500.copy(alpha = 0.07f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // أيقونة
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Indigo500, Violet500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "لوحة الإدارة",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "سجّل دخولك لإدارة البائعين والتقارير",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // بطاقة تسجيل الدخول
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "تسجيل الدخول",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Text(
                        "استخدم حساب Google المعتمد للوصول إلى لوحة الإدارة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // زر جوجل
                    GoogleSignInButton(
                        isLoading = state.isLoading,
                        onClick = { viewModel.signInWithGoogle(context) }
                    )

                    // رسالة خطأ
                    if (state.error != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = state.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "الدخول مقتصر على المسؤولين المعتمدين فقط",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Indigo500,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // أيقونة Google — SVG من drawable
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "تسجيل الدخول بـ Google",
                    color = Color(0xFF3C4043),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}