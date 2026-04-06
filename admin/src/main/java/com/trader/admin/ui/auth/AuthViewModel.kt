package com.trader.admin.ui.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    
    companion object {
        const val WEB_CLIENT_ID = "520815967269-lc6d7gc5ddrkfqfvuamq7b6fit2endal.apps.googleusercontent.com"
    }

    init {
        _state.update { it.copy(isAuthenticated = auth.currentUser != null) }
    }

    fun signInWithGoogle(context: Context) {
        _state.update { it.copy(isLoading = true, error = null) }

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(firebaseCredential).await()
                _state.update { it.copy(isLoading = false, isAuthenticated = true) }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "فشل تسجيل الدخول: ${e.message}")
                }
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                CredentialManager.create(context).clearCredentialState(
                    ClearCredentialStateRequest()
                )
            } catch (_: Exception) {}
            auth.signOut()
            _state.update { it.copy(isAuthenticated = false, error = null) }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}