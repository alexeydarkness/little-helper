package ru.littlehelper.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.littlehelper.app.data.ApiFailure
import ru.littlehelper.app.data.AppRepository
import ru.littlehelper.app.data.UserSearchResult

internal const val MAX_QUERY_LENGTH = 100

data class UserSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<UserSearchResult>? = null,
    val error: String? = null,
    val sessionExpired: Boolean = false,
) {
    val searchTerm: String get() = query.trim().removePrefix("@").trim()
    val canSearch: Boolean get() = searchTerm.length >= 2 && !loading && !sessionExpired
}

class UserSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.get(application)
    private val mutableState = MutableStateFlow(UserSearchState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null

    fun changeQuery(query: String) {
        val limitedQuery = query.take(MAX_QUERY_LENGTH)
        val current = mutableState.value
        if (limitedQuery == current.query || current.sessionExpired) return
        searchJob?.cancel()
        mutableState.value = UserSearchState(query = limitedQuery)
    }

    fun search() {
        val current = mutableState.value
        if (!current.canSearch) return
        mutableState.value = current.copy(loading = true, results = null, error = null)
        searchJob = viewModelScope.launch {
            try {
                val results = repository.searchUsers(current.searchTerm)
                mutableState.update { it.copy(loading = false, results = results) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiFailure) {
                mutableState.update {
                    it.copy(loading = false, sessionExpired = error.unauthorized, error = when (error.status) {
                        404 -> "Поиск пока недоступен"
                        else -> error.message
                    })
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Не удалось загрузить результаты") }
            }
        }
    }
}
