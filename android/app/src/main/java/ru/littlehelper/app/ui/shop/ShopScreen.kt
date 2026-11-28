package ru.littlehelper.app.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.littlehelper.app.data.ShopItem
import ru.littlehelper.app.data.ShopItemType
import ru.littlehelper.app.ui.theme.GreenTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: ShopViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val catalog = state.catalog
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }
    LaunchedEffect(catalog, state.error, state.message) {
        val notice = state.error ?: state.message
        if (catalog != null && notice != null) snackbarHost.showSnackbar(notice)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Магазин") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = viewModel::refresh, enabled = !state.busy) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить магазин")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GreenTopBar,
                titleContentColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.surface,
                actionIconContentColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }, snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        when {
            catalog == null && state.loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            catalog == null -> {
                Column(
                    Modifier.fillMaxWidth().padding(padding).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = viewModel::refresh) { Text("Повторить") }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "balance") {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Баланс", style = MaterialTheme.typography.labelLarge)
                            Text("${catalog.balance}", style = MaterialTheme.typography.headlineMedium)
                            Text("Игровые монеты", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (state.busy) item(key = "progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (catalog.items.isEmpty()) item(key = "empty") { Text("Каталог пока пуст") }
                for (type in ShopItemType.entries) {
                    val products = catalog.items.filter { it.type == type }
                    if (products.isEmpty()) continue
                    item(key = type.name) {
                        Text(
                            if (type == ShopItemType.FRAME) "Рамки" else "Фоны",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(products, key = { it.id }) { product ->
                        ShopItemCard(
                            item = product,
                            canAfford = product.price <= catalog.balance,
                            enabled = !state.busy,
                            purchasing = state.purchasingItemId == product.id,
                            onBuy = { viewModel.selectItem(product.id) },
                        )
                    }
                }
            }
        }
    }

    state.confirmation?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelConfirmation,
            title = { Text("Купить предмет?") },
            text = { Text("${item.name} · ${item.price} монет") },
            confirmButton = { TextButton(onClick = viewModel::confirmPurchase) { Text("Купить") } },
            dismissButton = { TextButton(onClick = viewModel::cancelConfirmation) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ShopItemCard(item: ShopItem, canAfford: Boolean, enabled: Boolean, purchasing: Boolean, onBuy: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        if (item.type == ShopItemType.FRAME) Icons.Default.AutoAwesome else Icons.Default.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).padding(10.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (item.owned) {
                Text(if (item.price == 0) "Доступно сразу" else "Уже куплено", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("Цена: ${item.price} монет", style = MaterialTheme.typography.labelLarge)
                Button(onClick = onBuy, enabled = enabled && canAfford, modifier = Modifier.fillMaxWidth()) {
                    Text(when {
                        purchasing -> "Покупка…"
                        !canAfford -> "Недостаточно монет"
                        else -> "Купить"
                    })
                }
            }
        }
    }
}
