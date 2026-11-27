package ru.littlehelper.app.ui.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.littlehelper.app.data.AppUser
import ru.littlehelper.app.data.Lesson
import ru.littlehelper.app.ui.common.Logo
import ru.littlehelper.app.ui.theme.GreenSlot
import ru.littlehelper.app.ui.theme.GreenTopBar
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val weekdayFormat = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("ru"))
private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))

@Composable
fun HomeScreen(onLogout: () -> Unit, onProfile: () -> Unit, onSearch: (Boolean) -> Unit,
               viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onLogout()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    HomeContent(state, viewModel::selectDate, viewModel::refresh, viewModel::openFreeSlot,
        viewModel::openLesson, onLogout, onProfile, onSearch)
    state.editor?.let { draft ->
        LessonEditor(draft, state.students, state.saving, state.editorError,
            viewModel::changeDraft, viewModel::save, viewModel::delete, viewModel::closeDialog)
    }
    state.details?.let { lesson ->
        LessonDetails(lesson, onDismiss = viewModel::closeDialog)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onDateChange: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onFreeSlot: (LocalTime, LocalTime) -> Unit,
    onLesson: (Lesson) -> Unit,
    onLogout: () -> Unit,
    onProfile: () -> Unit,
    onSearch: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Little helper") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GreenTopBar, titleContentColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    Box {
                        IconButton(onClick = { menuOpen = true }, enabled = !state.saving) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Профиль") }, onClick = {
                                menuOpen = false
                                onProfile()
                            })
                            state.user?.let { user ->
                                DropdownMenuItem(text = {
                                    Text(if (user.isTutor) "Найти ученика" else "Найти репетитора")
                                }, onClick = {
                                    menuOpen = false
                                    onSearch(user.isTutor)
                                })
                            }
                            DropdownMenuItem(text = { Text("Обновить") }, onClick = {
                                menuOpen = false
                                onRefresh()
                            })
                            DropdownMenuItem(text = { Text("Выйти") }, onClick = {
                                menuOpen = false
                                onLogout()
                            })
                            DropdownMenuItem(text = { Text("Закрыть приложение") }, onClick = {
                                menuOpen = false
                                activity?.finish()
                            })
                        }
                    }
                },
                actions = { Box(Modifier.padding(end = 12.dp)) { Logo(size = 36.dp) } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { calendarOpen = true }, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Выбрать дату")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDateChange(state.date.minusDays(1)) }, enabled = !state.saving) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий день")
                }
                Column(Modifier.weight(1f).clickable { calendarOpen = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.date.format(weekdayFormat).uppercase(Locale.forLanguageTag("ru")),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.date.format(dateFormat), style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { onDateChange(state.date.plusDays(1)) }, enabled = !state.saving) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующий день")
                }
            }
            state.user?.let { user ->
                val role = if (user.isTutor) "Репетитор" else "Ученик"
                Text("${user.fullName} · $role", modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Column(Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh) { Text("Повторить") }
                }
                state.user != null -> key(state.date) {
                    DaySchedule(state.lessons, state.user, onFreeSlot, onLesson)
                }
            }
        }
    }
    if (calendarOpen) {
        CalendarDialog(state.date, onDismiss = { calendarOpen = false }, onSelected = {
            calendarOpen = false
            onDateChange(it)
        })
    }
}

internal data class ScheduleEntry(val start: LocalTime, val end: LocalTime, val lesson: Lesson? = null)

internal fun scheduleEntries(lessons: List<Lesson>): List<ScheduleEntry> {
    val entries = mutableListOf<ScheduleEntry>()
    var minute = 0
    fun addFreeUntil(end: Int) {
        while (minute < end) {
            val next = minOf(minute + 60, end)
            entries.add(ScheduleEntry(LocalTime.ofSecondOfDay(minute * 60L), LocalTime.ofSecondOfDay(next * 60L)))
            minute = next
        }
    }
    lessons.sortedBy { it.startTime }.forEach { lesson ->
        addFreeUntil(lesson.startTime.toSecondOfDay() / 60)
        entries.add(ScheduleEntry(lesson.startTime, lesson.endTime, lesson))
        minute = lesson.endTime.toSecondOfDay() / 60
    }
    addFreeUntil(23 * 60 + 59)
    return entries
}

@Composable
private fun DaySchedule(lessons: List<Lesson>, user: AppUser,
                        onFreeSlot: (LocalTime, LocalTime) -> Unit, onLesson: (Lesson) -> Unit) {
    val entries = remember(lessons) { scheduleEntries(lessons) }
    val firstHour = minOf(5, lessons.minOfOrNull { it.startTime.hour } ?: 5)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex =
        entries.indexOfFirst { it.start.hour >= firstHour }.coerceAtLeast(0))
    LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(entries, key = { it.lesson?.let { lesson -> "lesson-${lesson.id}" } ?: "free-${it.start}" }) { entry ->
            val lesson = entry.lesson
            if (lesson == null) {
                Card(onClick = { onFreeSlot(entry.start, entry.end) }, enabled = user.isTutor,
                    colors = CardDefaults.cardColors(containerColor = GreenSlot)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        TimeLabel(entry.start.format(timeFormat))
                        Text("Свободно", modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
                        if (user.isTutor) Icon(Icons.Default.AddCircle, contentDescription = "Добавить занятие")
                    }
                }
            } else {
                val hue = ((lesson.participantId(user) * 137.508) % 360).toFloat()
                val color = Color.hsv(hue, 0.55f, 0.48f)
                Card(onClick = { onLesson(lesson) }) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.width(6.dp).fillMaxHeight().background(color))
                        Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${lesson.startTime.format(timeFormat)}–${lesson.endTime.format(timeFormat)}",
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (user.isTutor) Icon(Icons.Default.Edit, contentDescription = "Редактировать занятие", modifier = Modifier.size(20.dp))
                            }
                            Text(lesson.subject, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.participantName(user), color = color, fontWeight = FontWeight.Bold)
                            if (lesson.homework.isNotBlank()) Text("ДЗ · ${lesson.homework}", maxLines = 2)
                            Text(if (lesson.paid) "Оплачено" else "Ожидает оплаты", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeLabel(text: String) {
    Text(text, modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
}

@Composable
private fun LessonDetails(lesson: Lesson, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(lesson.subject) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${lesson.startTime.format(timeFormat)}–${lesson.endTime.format(timeFormat)}")
            Text("Репетитор · ${lesson.tutorName}")
            Text("Домашнее задание", fontWeight = FontWeight.Bold)
            Text(lesson.homework.ifBlank { "Пока не задано" })
            if (lesson.comment.isNotBlank()) {
                Text("Комментарий репетитора", fontWeight = FontWeight.Bold)
                Text(lesson.comment)
            }
            Text(if (lesson.paid) "Оплачено" else "Ожидает оплаты")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } })
}
