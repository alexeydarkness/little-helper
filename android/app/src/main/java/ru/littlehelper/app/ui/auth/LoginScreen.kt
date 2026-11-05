package ru.littlehelper.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.littlehelper.app.ui.theme.LittleHelperTheme

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) onLoggedIn()
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onRegisterClick = onRegisterClick,
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    AuthScaffold(title = "Вход") {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Email",
            icon = Icons.Filled.Email,
            error = state.emailError,
            keyboardType = KeyboardType.Email,
        )
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Пароль",
            icon = Icons.Filled.Lock,
            error = state.passwordError,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
        )
        Spacer(Modifier.height(16.dp))
        AuthErrorText(state.formError)
        AuthPrimaryButton(text = "Войти", loading = state.loading, onClick = onSubmit)
        Spacer(Modifier.height(16.dp))
        AuthSwitchLink(prefix = "Нет аккаунта?", action = "Зарегистрироваться", onClick = onRegisterClick)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginPreview() {
    LittleHelperTheme {
        LoginContent(LoginUiState(), {}, {}, {}, {})
    }
}
