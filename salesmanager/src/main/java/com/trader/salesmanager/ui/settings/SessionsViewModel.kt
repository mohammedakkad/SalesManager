package com.trader.salesmanager.ui.settings

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.local.dao.SessionDao
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: List<Session> = emptyList(),
    val currentDeviceId: String = "",
    val revokingSessionId: String? = null,
    val message: String? = null
)

class SessionsViewModel(
    private val sessionDao: SessionDao,
    private val firebaseSyncService: FirebaseSyncService,
    private val merchantCode: String,
    application: Application
) : AndroidViewModel(application) {

    private val currentDeviceId = getHardwareId(application)
    private val revokingSessionId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SessionsUiState> = combine(
        sessionDao.getAllSessions().map {
            sessions -> sessions.map {
                it.toDomain()
            }
        },
        revokingSessionId,
        message
    ) {
        sessions, revokingId, uiMessage ->
        SessionsUiState(
            sessions = sessions,
            currentDeviceId = currentDeviceId,
            revokingSessionId = revokingId,
            message = uiMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SessionsUiState(currentDeviceId = currentDeviceId)
    )

    fun revokeSession(sessionId: String) {
        if (sessionId.isBlank() || merchantCode.isBlank()) {
            message.value = "تعذر تحديد الجلسة الحالية"
            return
        }

        viewModelScope.launch {
            revokingSessionId.value = sessionId
            runCatching {
                firebaseSyncService.revokeSession(merchantCode, sessionId)
            }.onSuccess {
                message.value = "تم تسجيل خروج الجهاز بنجاح"
            }.onFailure {
                error ->
                message.value = error.message ?: "تعذر تسجيل خروج الجهاز حالياً"
            }
            revokingSessionId.value = null
        }
    }

    fun clearMessage() {
        message.value = null
    }

    @SuppressLint("HardwareIds")
    private fun getHardwareId(application: Application): String = Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "unknown_device"
}
