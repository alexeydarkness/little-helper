package ru.littlehelper.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.littlehelper.app.ui.theme.LittleHelperTheme

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) onRegistered()
    }

    RegisterContent(
        state = state,
        onChange = viewModel::onChange,
        onRoleChange = viewModel::onRoleChange,
        onSubmit = viewModel::submit,
        onLoginClick = onLoginClick,
    )
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    onChange: (RegisterField, String) -> Unit,
    onRoleChange: (Role) -> Unit,
    onSubmit: () -> Unit,
    onLoginClick: () -> Unit,
) {
    AuthScaffold(title = "Регистрация") {
        AuthTextField(
            value = state.username,
            onValueChange = { onChange(RegisterField.USERNAME, it) },
            label = "Никнейм",
            icon = Icons.Filled.Person,
            error = state.errors[RegisterField.USERNAME],
        )
        AuthTextField(
            value = state.fullName,
            onValueChange = { onChange(RegisterField.FULL_NAME, it) },
            label = "Полное имя",
            icon = Icons.Filled.Badge,
            error = state.errors[RegisterField.FULL_NAME],
        )
        AuthTextField(
            value = state.phone,
            onValueChange = { onChange(RegisterField.PHONE, it) },
            label = "Телефон",
            icon = Icons.Filled.Phone,
            error = state.errors[RegisterField.PHONE],
            keyboardType = KeyboardType.Phone,
        )
        RolePicker(role = state.role, onRoleChange = onRoleChange)
        AuthTextField(
            value = state.email,
            onValueChange = { onChange(RegisterField.EMAIL, it) },
            label = "Email",
            icon = Icons.Filled.Email,
            error = state.errors[RegisterField.EMAIL],
            keyboardType = KeyboardType.Email,
        )
        AuthTextField(
            value = state.password,
            onValueChange = { onChange(RegisterField.PASSWORD, it) },
            label = "Пароль",
            icon = Icons.Filled.Lock,
            error = state.errors[RegisterField.PASSWORD],
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
        )
        Spacer(Modifier.height(16.dp))
        AuthErrorText(state.formError)
        AuthPrimaryButton(text = "Зарегистрироваться", loading = state.loading, onClick = onSubmit)
        Spacer(Modifier.height(16.dp))
        AuthSwitchLink(prefix = "Уже есть аккаунт?", action = "Войти", onClick = onLoginClick)
    }
}

/** Выпадающий список «Ученик / Репетитор». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RolePicker(role: Role, onRoleChange: (Role) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = role.title,
            onValueChange = {},
            readOnly = true,
            label = { Text("Роль") },
            leadingIcon = { Icon(Icons.Filled.AssignmentInd, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(8.dp),
            colors = authFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Role.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        onRoleChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RegisterPreview() {
    LittleHelperTheme {
        RegisterContent(RegisterUiState(), { _, _ -> }, {}, {}, {})
    }
}
