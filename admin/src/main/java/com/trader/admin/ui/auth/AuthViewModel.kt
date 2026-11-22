package com.trader.admin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val passwordVisible: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isAuthenticated = auth.currentUser != null) }
    }

    fun updateEmail(email: String) = _state.update { it.copy(email = email, error = null) }
    fun updatePassword(password: String) = _state.update { it.copy(password = password, error = null) }
    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun signIn() {
        val email = _state.value.email.trim()
        val password = _state.value.password.trim()

        if (email.isEmpty()) { _state.update { it.copy(error = "أدخل البريد الإلكتروني") }; return }
        if (password.isEmpty()) { _state.update { it.copy(error = "أدخل كلمة المرور") }; return }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "البريد أو كلمة المرور غير صحيحة") }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _state.update { it.copy(isAuthenticated = false, email = "", password = "", error = null) }
    }
}