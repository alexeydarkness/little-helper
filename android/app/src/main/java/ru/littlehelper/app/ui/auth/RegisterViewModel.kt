package ru.littlehelper.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.littlehelper.app.data.AppRepository

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

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.get(application)

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
            try {
                repository.register(s.username, s.fullName, s.phone, s.role.name, s.email, s.password)
                _state.update { it.copy(loading = false, success = true, password = "") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, formError = error.message ?: "Не удалось зарегистрироваться") }
            }
        }
    }
}
