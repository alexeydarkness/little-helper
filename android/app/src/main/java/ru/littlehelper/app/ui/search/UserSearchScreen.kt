package ru.littlehelper.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.littlehelper.app.data.UserSearchResult
import ru.littlehelper.app.ui.theme.GreenTopBar
import ru.littlehelper.app.ui.theme.LittleHelperTheme

@Composable
fun UserSearchScreen(
    isTutor: Boolean,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: UserSearchViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }
    UserSearchContent(isTutor, state, onBack, viewModel::changeQuery, viewModel::search)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSearchContent(
    isTutor: Boolean,
    state: UserSearchState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        if (state.canSearch) {
            focusManager.clearFocus()
            onSearch()
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isTutor) "Поиск учеников" else "Поиск репетиторов") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GreenTopBar,
                titleContentColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Имя или никнейм") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить запрос")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = submit, enabled = state.canSearch, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.error == null) "Найти" else "Повторить")
            }
            val results = state.results
            val error = state.error
            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                results == null -> Text("Введи не менее двух символов", style = MaterialTheme.typography.bodyMedium)
                results.isEmpty() -> Text("Никого не нашли. Попробуй другое имя или никнейм.")
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { user ->
                        SearchResultCard(user)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(user: UserSearchResult) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Person, null, Modifier.size(44.dp).padding(10.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                val role = if (user.role == "TUTOR") "Репетитор" else "Ученик"
                Text("@${user.username} · $role", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchPreview() {
    LittleHelperTheme {
        UserSearchContent(
            isTutor = false,
            state = UserSearchState(query = "Анна", results = listOf(UserSearchResult(1, "Анна Петрова", "anna", "TUTOR"))),
            onBack = {},
            onQueryChange = {},
            onSearch = {},
        )
    }
}
