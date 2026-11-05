package ru.littlehelper.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ru.littlehelper.app.ui.common.Logo
import ru.littlehelper.app.ui.theme.GreenButton
import ru.littlehelper.app.ui.theme.GreenCard
import ru.littlehelper.app.ui.theme.GreenText
import ru.littlehelper.app.ui.theme.GreenTopBar
import ru.littlehelper.app.ui.theme.GreyOutline

// Общие элементы экранов входа и регистрации. Используются в LoginScreen и RegisterScreen.

/** Тёмно-зелёный фон + светлая карточка с логотипом и заголовком. */
@Composable
fun AuthScaffold(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenTopBar)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = GreenCard,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Logo()
                Spacer(Modifier.height(20.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = GreenText)
                Spacer(Modifier.height(24.dp))
                content()
            }
        }
    }
}

/** Поле ввода с иконкой слева и текстом ошибки под ним. Для пароля — кнопка «показать/скрыть». */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(8.dp),
        colors = authFieldColors(),
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
    )
}

/** Цвета полей ввода в стиле макетов. */
@Composable
fun authFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenButton,
    unfocusedBorderColor = GreyOutline,
    focusedLabelColor = GreenButton,
    focusedLeadingIconColor = GreenButton,
    cursorColor = GreenButton,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

/** Большая тёмно-зелёная кнопка. Пока loading = true, показывает крутилку и не нажимается. */
@Composable
fun AuthPrimaryButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = GreenCard, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, color = GreenCard)
        }
    }
}

/** Ссылка внизу карточки: «Нет аккаунта? Зарегистрироваться». Вторая часть — жирная. */
@Composable
fun AuthSwitchLink(prefix: String, action: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            buildAnnotatedString {
                append("$prefix ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(action) }
            },
            color = GreenText,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Общая ошибка формы (например, «Неверный email или пароль»). Если message = null — ничего не рисует. */
@Composable
fun AuthErrorText(message: String?) {
    if (message != null) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}
