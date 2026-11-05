package ru.littlehelper.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Роль пользователя. На сервер уйдёт как STUDENT / TUTOR. */
enum class Role(val title: String) {
    STUDENT("Ученик"),
    TUTOR("Репетитор"),
}

/** Поля формы — по ним храним ошибки. */
enum class RegisterField { USERNAME, FULL_NAME, PHONE, EMAIL, PASSWORD }

data class RegisterUiState(
    val username: String = "",
    val fullName: String = "",
    val phone: String = "",
    val role: Role = Role.STUDENT,
    val email: String = "",
    val password: String = "",
    val errors: Map<RegisterField, String> = emptyMap(),
    val formError: String? = null,
    val loading: Boolean = false,
    val success: Boolean = false,
)

class RegisterViewModel : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** Одно поле изменилось: сохраняем значение и убираем его ошибку. */
    fun onChange(field: RegisterField, value: String) = _state.update {
        val cleared = it.copy(errors = it.errors - field, formError = null)
        when (field) {
            RegisterField.USERNAME -> cleared.copy(username = value)
            RegisterField.FULL_NAME -> cleared.copy(fullName = value)
            RegisterField.PHONE -> cleared.copy(phone = value)
            RegisterField.EMAIL -> cleared.copy(email = value)
            RegisterField.PASSWORD -> cleared.copy(password = value)
        }
    }

    fun onRoleChange(role: Role) = _state.update { it.copy(role = role) }

    fun submit() {
        val s = _state.value
        if (s.loading) return

        // Собираем ошибки всех полей сразу, чтобы пользователь увидел их одновременно
        val errors = buildMap {
            AuthValidation.username(s.username)?.let { put(RegisterField.USERNAME, it) }
            AuthValidation.fullName(s.fullName)?.let { put(RegisterField.FULL_NAME, it) }
            AuthValidation.phone(s.phone)?.let { put(RegisterField.PHONE, it) }
            AuthValidation.email(s.email)?.let { put(RegisterField.EMAIL, it) }
            AuthValidation.newPassword(s.password)?.let { put(RegisterField.PASSWORD, it) }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            // TODO(спринт 4): заменить на запрос к серверу POST /api/v1/auth/register
            delay(800)
            _state.update { it.copy(loading = false, success = true) }
        }
    }
}
