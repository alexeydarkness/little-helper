package ru.littlehelper.app.ui.auth

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// TODO(#20): заменить заглушку на экран входа
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onRegisterClick: () -> Unit) {
    AuthScaffold(title = "Вход") {
        Text("Экран входа — задача #20")
        AuthPrimaryButton(text = "Войти", loading = false, onClick = onLoggedIn)
        AuthSwitchLink(prefix = "Нет аккаунта?", action = "Зарегистрироваться", onClick = onRegisterClick)
    }
}
