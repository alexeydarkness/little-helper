package ru.littlehelper.app.ui.profile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.littlehelper.app.ui.common.Logo
import ru.littlehelper.app.ui.theme.GreenTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onSessionExpired: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::photo)
    }
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }
    BackHandler(enabled = state.editing || state.saving) {
        if (!state.saving) viewModel.cancel()
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (state.editing) "Редактировать профиль" else "Профиль") },
            navigationIcon = {
                IconButton(onClick = { if (state.editing) viewModel.cancel() else onBack() }, enabled = !state.saving) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }, actions = {
                if (state.profile != null && !state.editing) IconButton(onClick = viewModel::edit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать профиль")
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenTopBar,
                titleContentColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.surface,
                actionIconContentColor = MaterialTheme.colorScheme.surface))
    }) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val profile = if (state.editing) state.draft else state.profile
                profile?.let { value ->
                    val bitmap = remember(value.avatarBase64) {
                        runCatching {
                            val bytes = Base64.decode(value.avatarBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }.getOrNull()
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val photoModifier = Modifier.size(112.dp).clip(CircleShape)
                            .clickable(enabled = state.editing && !state.saving) { picker.launch("image/*") }
                        if (bitmap == null) Logo(modifier = photoModifier, size = 112.dp)
                        else Image(bitmap, contentDescription = "Аватар", modifier = photoModifier, contentScale = ContentScale.Crop)
                        if (state.editing) {
                            TextButton(onClick = { picker.launch("image/*") }, enabled = !state.saving) { Text("Выбрать фото") }
                            if (value.avatarBase64.isNotEmpty()) TextButton(onClick = {
                                viewModel.change(value.copy(avatarBase64 = ""))
                            }, enabled = !state.saving) { Text("Удалить фото") }
                        } else {
                            Text(value.fullName, style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(if (value.role == "TUTOR") "Репетитор" else "Ученик")
                    }
                    if (state.editing) {
                        ProfileField("Никнейм", value.username, !state.saving) {
                            viewModel.change(value.copy(username = it))
                        }
                        ProfileField("Email", value.email, !state.saving, KeyboardType.Email) {
                            viewModel.change(value.copy(email = it))
                        }
                        ProfileField("Полное имя", value.fullName, !state.saving) {
                            viewModel.change(value.copy(fullName = it))
                        }
                        ProfileField("Телефон", value.phone, !state.saving, KeyboardType.Phone) {
                            viewModel.change(value.copy(phone = it))
                        }
                        if (state.saving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                            Text("Сохранить")
                        }
                        OutlinedButton(onClick = viewModel::cancel, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                            Text("Отмена")
                        }
                    } else {
                        ProfileInfo("Мой баланс", "${value.balance} монет")
                        ProfileInfo("Никнейм", value.username)
                        ProfileInfo("Email", value.email)
                        ProfileInfo("Телефон", value.phone.ifBlank { "Не указан" })
                        val date = runCatching {
                            Instant.parse(value.createdAt).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        }.getOrDefault(value.createdAt)
                        ProfileInfo("Дата регистрации", date)
                    }
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    if (state.profile == null) TextButton(onClick = viewModel::load) { Text("Повторить") }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, enabled: Boolean,
                         keyboardType: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, enabled = enabled,
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ProfileInfo(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
