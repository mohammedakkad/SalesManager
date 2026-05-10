package com.trader.salesmanager.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.manager.EmployeeSessionManager
import com.trader.core.domain.repository.EmployeeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the PIN lock screen.
 *
 * @property pin     digits typed so far (kept in memory only).
 * @property maxLen  expected PIN length (4 by default).
 * @property loading set while we run the lookup against [EmployeeRepository].
 * @property hasError last attempt was wrong — drives the shake/red animation.
 * @property attempts count of consecutive wrong attempts (used for soft cooldown).
 */
data class PinLockUiState(
    val pin: String = "",
    val maxLen: Int = 4,
    val loading: Boolean = false,
    val hasError: Boolean = false,
    val attempts: Int = 0
)

/**
 * One-shot UI events emitted by [PinLockViewModel] — collected by the screen
 * to trigger haptics, navigation and the shake animation.
 */
sealed interface PinLockEvent {
    data class Success(val employeeName: String, val isAdmin: Boolean) : PinLockEvent
    object WrongPin : PinLockEvent
}

class PinLockViewModel(
    private val sessionManager: EmployeeSessionManager,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PinLockUiState())
    val state: StateFlow<PinLockUiState> = _state.asStateFlow()

    private val _events = Channel<PinLockEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Make sure the default admin (PIN 0000) exists for first run.
        viewModelScope.launch {
            runCatching { employeeRepository.ensureDefaultAdmin() }
        }
    }

    fun onDigit(digit: Int) {
        val current = _state.value
        if (current.loading) return
        if (current.pin.length >= current.maxLen) return

        // Typing after an error clears the error visuals.
        val nextPin = current.pin + digit.toString()
        _state.update { it.copy(pin = nextPin, hasError = false) }

        if (nextPin.length == current.maxLen) {
            verify(nextPin)
        }
    }

    fun onBackspace() {
        val current = _state.value
        if (current.loading || current.pin.isEmpty()) return
        _state.update {
            it.copy(pin = it.pin.dropLast(1), hasError = false)
        }
    }

    fun onClear() {
        if (_state.value.loading) return
        _state.update { it.copy(pin = "", hasError = false) }
    }

    private fun verify(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val ok = runCatching { sessionManager.loginWithPin(pin) }
                .getOrDefault(false)

            if (ok) {
                val employee = sessionManager.currentActiveEmployee.value
                _state.update { it.copy(loading = false, hasError = false, attempts = 0) }
                _events.trySend(
                    PinLockEvent.Success(
                        employeeName = employee?.name.orEmpty(),
                        isAdmin = employee?.role?.name == "ADMIN"
                    )
                )
            } else {
                _state.update {
                    it.copy(
                        loading = false,
                        hasError = true,
                        attempts = it.attempts + 1
                    )
                }
                _events.trySend(PinLockEvent.WrongPin)
            }
        }
    }
}
