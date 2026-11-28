package ru.littlehelper.app.ui.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.littlehelper.app.data.ApiFailure
import ru.littlehelper.app.data.AppRepository
import ru.littlehelper.app.data.ShopCatalog
import ru.littlehelper.app.data.ShopItem

data class ShopUiState(
    val catalog: ShopCatalog? = null,
    val loading: Boolean = false,
    val confirmation: ShopItem? = null,
    val purchasingItemId: String? = null,
    val error: String? = null,
    val message: String? = null,
    val sessionExpired: Boolean = false,
) {
    val busy: Boolean get() = loading || purchasingItemId != null
}

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.get(application)
    private val mutableState = MutableStateFlow(ShopUiState())
    val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val current = mutableState.value
        if (current.busy || current.confirmation != null || current.sessionExpired) return
        mutableState.value = current.copy(loading = true, error = null, message = null)
        viewModelScope.launch {
            try {
                val catalog = repository.shopCatalog()
                mutableState.update { it.copy(catalog = catalog, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error, "Не удалось загрузить магазин")
            }
        }
    }

    fun selectItem(itemId: String) {
        val current = mutableState.value
        val catalog = current.catalog ?: return
        if (current.busy || current.sessionExpired) return
        val item = catalog.items.firstOrNull { it.id == itemId } ?: return
        if (item.owned) return
        if (item.price > catalog.balance) {
            mutableState.value = current.copy(error = "Недостаточно монет", message = null)
            return
        }
        mutableState.value = current.copy(confirmation = item, error = null, message = null)
    }

    fun cancelConfirmation() {
        mutableState.update { it.copy(confirmation = null) }
    }

    fun confirmPurchase() {
        val current = mutableState.value
        val catalog = current.catalog ?: return
        val item = current.confirmation ?: return
        if (current.busy || current.sessionExpired) return
        mutableState.value = current.copy(confirmation = null, purchasingItemId = item.id)
        viewModelScope.launch {
            try {
                val result = repository.buyShopItem(item.id)
                val updatedItems = catalog.items.map { product ->
                    if (product.id == result.itemId) product.copy(owned = true) else product
                }
                mutableState.update {
                    it.copy(
                        catalog = ShopCatalog(result.balance, updatedItems),
                        purchasingItemId = null,
                        message = if (result.alreadyOwned) "Предмет уже куплен" else "Покупка выполнена",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error, "Не удалось завершить покупку")
            }
        }
    }

    private fun showError(error: Exception, fallback: String) {
        val failure = error as? ApiFailure
        mutableState.update {
            it.copy(
                loading = false,
                purchasingItemId = null,
                sessionExpired = failure?.unauthorized == true,
                error = if (failure?.status == 409) "Недостаточно монет" else failure?.message ?: fallback,
            )
        }
    }
}
