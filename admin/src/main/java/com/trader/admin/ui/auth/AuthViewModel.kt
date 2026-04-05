package com.trader.admin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class AuthUiState(
    val phone: String = "",
    val otp: String = "",
    val step: AuthStep = AuthStep.PHONE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

enum class AuthStep { PHONE, OTP }

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var verificationId: String? = null

    init {
        // Check if already logged in
        _state.update { it.copy(isAuthenticated = auth.currentUser != null) }
    }

    fun updatePhone(phone: String) = _state.update { it.copy(phone = phone, error = null) }
    fun updateOtp(otp: String) = _state.update { it.copy(otp = otp, error = null) }

    fun sendOtp(activity: android.app.Activity) {
        val phone = _state.value.phone.trim()
        if (phone.isEmpty()) { _state.update { it.copy(error = "أدخل رقم الهاتف") }; return }
        _state.update { it.copy(isLoading = true, error = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _state.update { it.copy(isLoading = false, error = "فشل الإرسال: ${e.message}") }
            }
            override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = vId
                _state.update { it.copy(isLoading = false, step = AuthStep.OTP) }
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    fun verifyOtp() {
        val vId = verificationId ?: return
        val otp  = _state.value.otp.trim()
        if (otp.length < 6) { _state.update { it.copy(error = "كود التحقق 6 أرقام") }; return }
        _state.update { it.copy(isLoading = true) }
        val credential = PhoneAuthProvider.getCredential(vId, otp)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "كود غير صحيح") }
            }
        }
    }

    fun signOut() { auth.signOut(); _state.update { it.copy(isAuthenticated = false, step = AuthStep.PHONE, phone = "", otp = "") } }
}
