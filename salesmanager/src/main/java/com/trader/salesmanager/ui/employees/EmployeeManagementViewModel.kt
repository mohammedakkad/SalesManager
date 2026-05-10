package com.trader.salesmanager.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Employee
import com.trader.core.domain.model.EmployeeRole
import com.trader.core.domain.repository.EmployeeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Reasons "add employee" can fail validation. */
enum class AddEmployeeError {
    NONE,
    NAME_BLANK,
    PIN_INVALID,
    PIN_TAKEN
}

/** Form state for the "add employee" bottom sheet. */
data class AddEmployeeForm(
    val name: String = "",
    val pin: String = "",
    val role: EmployeeRole = EmployeeRole.CASHIER,
    val submitting: Boolean = false,
    val error: AddEmployeeError = AddEmployeeError.NONE
) {
    val canSubmit: Boolean
        get() = !submitting && name.trim().length >= 2 && pin.length == 4
}

/** One-shot UI events from the management screen. */
sealed interface EmployeeManagementEvent {
    object EmployeeAdded : EmployeeManagementEvent
    object EmployeeDeleted : EmployeeManagementEvent
    data class Failure(val message: String) : EmployeeManagementEvent
}

class EmployeeManagementViewModel(
    private val repository: EmployeeRepository
) : ViewModel() {

    val employees: StateFlow<List<Employee>> = repository
        .observeEmployees()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _form = MutableStateFlow(AddEmployeeForm())
    val form: StateFlow<AddEmployeeForm> = _form.asStateFlow()

    private val _events = Channel<EmployeeManagementEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.ensureDefaultAdmin() }
        }
    }

    // ── form mutators ───────────────────────────────────────────
    fun onNameChange(value: String) {
        _form.update { it.copy(name = value, error = AddEmployeeError.NONE) }
    }

    fun onPinChange(value: String) {
        // Only allow up to 4 digits.
        val sanitized = value.filter { it.isDigit() }.take(4)
        _form.update { it.copy(pin = sanitized, error = AddEmployeeError.NONE) }
    }

    fun onRoleChange(role: EmployeeRole) {
        _form.update { it.copy(role = role) }
    }

    fun resetForm() {
        _form.value = AddEmployeeForm()
    }

    // ── actions ─────────────────────────────────────────────────
    fun submit() {
        val current = _form.value
        if (current.submitting) return

        val name = current.name.trim()
        val pin = current.pin

        if (name.length < 2) {
            _form.update { it.copy(error = AddEmployeeError.NAME_BLANK) }
            return
        }
        if (pin.length != 4) {
            _form.update { it.copy(error = AddEmployeeError.PIN_INVALID) }
            return
        }

        viewModelScope.launch {
            _form.update { it.copy(submitting = true, error = AddEmployeeError.NONE) }
            val existing = runCatching { repository.getByPin(pin) }.getOrNull()
            if (existing != null) {
                _form.update {
                    it.copy(submitting = false, error = AddEmployeeError.PIN_TAKEN)
                }
                _events.trySend(
                    EmployeeManagementEvent.Failure("هذا الرمز مستخدم بالفعل")
                )
                return@launch
            }

            runCatching {
                repository.addOrUpdate(
                    Employee(
                        name = name,
                        pinCode = pin,
                        role = current.role
                    )
                )
            }.onSuccess {
                resetForm()
                _events.trySend(EmployeeManagementEvent.EmployeeAdded)
            }.onFailure { t ->
                _form.update { it.copy(submitting = false) }
                _events.trySend(
                    EmployeeManagementEvent.Failure(t.message ?: "تعذّر حفظ الموظف")
                )
            }
        }
    }

    fun delete(employee: Employee) {
        // Refuse to delete the seeded default admin to avoid lock-out.
        if (employee.role == EmployeeRole.ADMIN && employee.id.startsWith("admin_")) {
            _events.trySend(
                EmployeeManagementEvent.Failure("لا يمكن حذف المدير الافتراضي")
            )
            return
        }
        viewModelScope.launch {
            runCatching { repository.deleteById(employee.id) }
                .onSuccess { _events.trySend(EmployeeManagementEvent.EmployeeDeleted) }
                .onFailure {
                    _events.trySend(
                        EmployeeManagementEvent.Failure(
                            it.message ?: "تعذّر حذف الموظف"
                        )
                    )
                }
        }
    }
}
