package com.trader.salesmanager.ui.settings.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.dao.SessionDao
import com.trader.core.domain.model.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SessionsViewModel(
    private val sessionDao: SessionDao,
    private val merchantCode: String,
    val currentDeviceId: String
) : ViewModel() {

    private val sessionsReference = FirebaseDatabase.getInstance()
        .getReference("merchants")
        .child(merchantCode)
        .child("sessions")

    val activeSessions: StateFlow<List<Session>> = sessionDao
        .getAllSessions()
        .map { sessions -> sessions.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun revokeSession(sessionId: String) {
        if (sessionId.isBlank() || merchantCode.isBlank()) return

        viewModelScope.launch {
            runCatching {
                sessionsReference.child(sessionId).removeValue().await()
                sessionDao.deleteSessionById(sessionId)
            }
        }
    }
}
