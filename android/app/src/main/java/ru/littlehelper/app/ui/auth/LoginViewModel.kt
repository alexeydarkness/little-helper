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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val success: Boolean = false,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.get(application)

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun submit() {
        val current = _state.value
        if (current.loading) return

        val emailError = AuthValidation.email(current.email)
        val passwordError = AuthValidation.loginPassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                repository.login(current.email, current.password)
                _state.update { it.copy(loading = false, success = true, password = "") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, formError = error.message ?: "Не удалось войти") }
            }
        }
    }
}
