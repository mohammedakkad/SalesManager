package com.trader.salesmanager.ui.settings.sessions

import com.trader.core.domain.repository.MerchantContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.dao.SessionDao
import com.trader.core.domain.model.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SessionsViewModel(
    private val sessionDao: SessionDao,
    private val merchant: MerchantContext
) : ViewModel() {

    val activeSessions: StateFlow<List<Session>> = sessionDao
        .getAllSessions()
        .map { sessions -> sessions.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _revoking = MutableStateFlow<Set<String>>(emptySet())
    val revoking: StateFlow<Set<String>> = _revoking.asStateFlow()
    private val _revokeError = MutableStateFlow<String?>(null)
    val revokeError: StateFlow<String?> = _revokeError.asStateFlow()

    fun revokeSession(sessionId: String) {
        if (sessionId.isBlank() || sessionId in _revoking.value) return
        _revoking.update { it + sessionId }
        _revokeError.value = null
        viewModelScope.launch {
            try {
                // Resolve the merchant at revoke time: the node must belong to the merchant active now.
                val merchantCode = merchant.currentCode()
                if (merchantCode.isBlank()) return@launch
                FirebaseDatabase.getInstance().getReference("merchants").child(merchantCode).child("sessions")
                    .child(sessionId).removeValue().await()
                sessionDao.deleteSessionById(sessionId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _revokeError.value = "تعذر إنهاء الجلسة، حاول مجددًا"
            } finally {
                _revoking.update { it - sessionId }
            }
        }
    }
}
