package com.trader.admin.ui.auth

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Navy800, Navy950, Color(0xFF0D0D1A)))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(320.dp).align(Alignment.TopCenter)
                .offset(y = (-60).dp).clip(CircleShape)
                .background(Indigo500.copy(alpha = 0.07f))
        )
        Box(
            modifier = Modifier.size(220.dp).align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp).clip(CircleShape)
                .background(Violet500.copy(alpha = 0.07f))
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Indigo500, Violet500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AdminPanelSettings, null,
                    tint = Color.White, modifier = Modifier.size(52.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text("لوحة الإدارة",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("سجّل دخولك لإدارة البائعين والتقارير",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400, textAlign = TextAlign.Center)

            Spacer(Modifier.height(40.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("تسجيل الدخول",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, color = Color.White)

                    // البريد الإلكتروني
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text("البريد الإلكتروني", color = Slate400) },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Indigo400) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = state.error != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Indigo400
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // كلمة المرور
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("كلمة المرور", color = Slate400) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Indigo400) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisible) {
                                Icon(
                                    if (state.passwordVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    null, tint = Slate400
                                )
                            }
                        },
                        visualTransformation = if (state.passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = state.error != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Indigo400
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // رسالة خطأ
                    if (state.error != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(state.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(10.dp))
                        }
                    }

                    // زر الدخول
                    Button(
                        onClick = viewModel::signIn,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                    ) {
                        if (state.isLoading)
                            CircularProgressIndicator(Modifier.size(22.dp),
                                color = Color.White, strokeWidth = 2.5.dp)
                        else
                            Text("دخول", fontWeight = FontWeight.Bold,
                                color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("الدخول مقتصر على المسؤولين المعتمدين فقط",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600, textAlign = TextAlign.Center)
        }
    }
}