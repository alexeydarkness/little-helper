package ru.littlehelper.app.ui.home

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
import ru.littlehelper.app.data.AppUser
import ru.littlehelper.app.data.Lesson
import ru.littlehelper.app.data.LessonDraft
import ru.littlehelper.app.data.Student
import java.time.LocalDate
import java.time.LocalTime

data class HomeUiState(
    val date: LocalDate = LocalDate.now(),
    val user: AppUser? = null,
    val students: List<Student> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val editor: LessonDraft? = null,
    val editorError: String? = null,
    val details: Lesson? = null,
    val sessionExpired: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.get(application)
    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun selectDate(date: LocalDate) {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(date = date, lessons = emptyList()) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        val date = mutableState.value.date
        mutableState.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val user = repository.currentUser()
                val students = if (user.isTutor) repository.students() else emptyList()
                val lessons = repository.lessons(date)
                mutableState.update { it.copy(user = user, students = students, lessons = lessons, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun openFreeSlot(start: LocalTime, end: LocalTime) {
        val current = mutableState.value
        if (current.user?.isTutor != true || current.loading || current.saving) return
        mutableState.update { it.copy(editor = LessonDraft(date = current.date,
            startTime = start, endTime = end), editorError = null) }
    }

    fun openLesson(lesson: Lesson) {
        if (mutableState.value.user?.isTutor == true) {
            mutableState.update { it.copy(editor = LessonDraft(lesson.id, lesson.studentId, lesson.subject,
                lesson.date, lesson.startTime, lesson.endTime, lesson.homework, lesson.comment, lesson.paid),
                editorError = null) }
        } else {
            mutableState.update { it.copy(details = lesson) }
        }
    }

    fun changeDraft(draft: LessonDraft) {
        if (!mutableState.value.saving) mutableState.update { it.copy(editor = draft, editorError = null) }
    }

    fun closeDialog() {
        if (!mutableState.value.saving) mutableState.update { it.copy(editor = null, details = null, editorError = null) }
    }

    fun save() {
        val draft = mutableState.value.editor ?: return
        val message = when {
            draft.studentId == null -> "Выберите ученика"
            draft.subject.isBlank() -> "Введите предмет"
            !draft.endTime.isAfter(draft.startTime) -> "Конец занятия должен быть позже начала"
            else -> null
        }
        if (message != null) {
            mutableState.update { it.copy(editorError = message) }
            return
        }
        mutate { repository.saveLesson(draft) }
    }

    fun delete() {
        val id = mutableState.value.editor?.id ?: return
        mutate { repository.deleteLesson(id) }
    }

    private fun mutate(action: suspend () -> Unit) {
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, editorError = null) }
        viewModelScope.launch {
            try {
                action()
                mutableState.update { it.copy(saving = false, editor = null) }
                refresh()
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
        mutableState.update {
            val message = error.message ?: "Не удалось загрузить расписание"
            it.copy(loading = false, saving = false, sessionExpired = expired,
                error = if (it.editor == null) message else it.error,
                editorError = if (it.editor != null) message else null)
        }
    }
}
