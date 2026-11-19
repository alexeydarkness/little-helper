package ru.littlehelper.app.ui.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.littlehelper.app.data.ApiFailure
import ru.littlehelper.app.data.AppRepository
import ru.littlehelper.app.data.UserProfile
import java.io.ByteArrayOutputStream

data class ProfileState(
    val profile: UserProfile? = null,
    val draft: UserProfile? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val editing: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.get(application)
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = repository.profile()
                mutableState.update { it.copy(profile = profile, draft = profile, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun edit() = mutableState.update { it.copy(editing = true, draft = it.profile, error = null) }
    fun cancel() = mutableState.update { it.copy(editing = false, draft = it.profile, error = null) }
    fun change(draft: UserProfile) {
        if (!mutableState.value.saving) mutableState.update { it.copy(draft = draft, error = null) }
    }

    fun save() {
        val current = mutableState.value
        val draft = current.draft ?: return
        if (current.saving) return
        if (draft.username.isBlank() || draft.email.isBlank() || draft.fullName.isBlank()) {
            mutableState.update { it.copy(error = "Заполните никнейм, email и имя") }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = repository.updateProfile(draft)
                mutableState.update { it.copy(profile = profile, draft = profile, editing = false, saving = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun photo(uri: Uri) {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val encoded = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                    require(options.outWidth > 0 && options.outHeight > 0) { "Не удалось прочитать фото" }
                    options.inSampleSize = 1
                    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) {
                        options.inSampleSize *= 2
                    }
                    options.inJustDecodeBounds = false
                    val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                        ?: error("Не удалось прочитать фото")
                    try {
                        val output = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    } finally {
                        bitmap.recycle()
                    }
                }
                mutableState.update { it.copy(draft = it.draft?.copy(avatarBase64 = encoded), saving = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    private fun fail(error: Exception) {
        val expired = error is ApiFailure && error.unauthorized
        if (expired) repository.logout()
        mutableState.update { it.copy(loading = false, saving = false, error = error.message ?: "Не удалось загрузить профиль",
            sessionExpired = expired) }
    }
}
