package ru.littlehelper.app.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.littlehelper.app.data.LessonDraft
import ru.littlehelper.app.data.Student
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonEditor(
    draft: LessonDraft,
    students: List<Student>,
    saving: Boolean,
    error: String?,
    onChange: (LessonDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var studentMenu by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth()) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (draft.id == null) "Новое занятие" else "Редактирование занятия",
                    style = MaterialTheme.typography.titleLarge)
                ExposedDropdownMenuBox(expanded = studentMenu, onExpandedChange = {
                    if (!saving) studentMenu = it
                }) {
                    OutlinedTextField(
                        value = students.firstOrNull { it.id == draft.studentId }?.fullName.orEmpty(),
                        onValueChange = {}, readOnly = true, enabled = !saving,
                        label = { Text("Ученик") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(studentMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = studentMenu, onDismissRequest = { studentMenu = false }) {
                        students.forEach { student ->
                            DropdownMenuItem(text = { Text("${student.fullName} · @${student.username}") }, onClick = {
                                onChange(draft.copy(studentId = student.id))
                                studentMenu = false
                            })
                        }
                    }
                }
                if (students.isEmpty()) Text("Пока нет учеников для выбора", color = MaterialTheme.colorScheme.error)
                OutlinedTextField(value = draft.subject, onValueChange = { if (it.length <= 100) onChange(draft.copy(subject = it)) },
                    label = { Text("Предмет") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { calendarOpen = true }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text(draft.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeButton("Начало", draft.startTime, !saving, Modifier.weight(1f)) { onChange(draft.copy(startTime = it)) }
                    TimeButton("Конец", draft.endTime, !saving, Modifier.weight(1f)) { onChange(draft.copy(endTime = it)) }
                }
                OutlinedTextField(value = draft.homework, onValueChange = {
                    if (it.length <= 2000) onChange(draft.copy(homework = it))
                }, label = { Text("Домашнее задание") }, minLines = 2, maxLines = 4,
                    enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = draft.comment, onValueChange = {
                    if (it.length <= 2000) onChange(draft.copy(comment = it))
                }, label = { Text("Комментарий") }, minLines = 2, maxLines = 4,
                    enabled = !saving, modifier = Modifier.fillMaxWidth())
                Row {
                    Checkbox(checked = draft.paid, onCheckedChange = { onChange(draft.copy(paid = it)) }, enabled = !saving)
                    Text("Занятие оплачено", modifier = Modifier.padding(top = 12.dp))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (saving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Button(onClick = onSave, enabled = !saving && students.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (draft.id != null) {
                        TextButton(onClick = { confirmDelete = true }, enabled = !saving) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") }
                }
            }
        }
    }
    if (calendarOpen) CalendarDialog(draft.date, onDismiss = { calendarOpen = false }, onSelected = {
        calendarOpen = false
        onChange(draft.copy(date = it))
    })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Удалить занятие?") },
        text = { Text("Занятие будет удалено из расписания обоих участников.") },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false
            onDelete()
        }) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } })
}

@Composable
private fun TimeButton(label: String, time: LocalTime, enabled: Boolean, modifier: Modifier,
                       onSelected: (LocalTime) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        TimePickerDialog(context, { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) },
            time.hour, time.minute, true).show()
    }, enabled = enabled, modifier = modifier) {
        Text("$label ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(date: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = {
            picker.selectedDateMillis?.let { onSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
        }, enabled = picker.selectedDateMillis != null) { Text("Выбрать") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }) {
        DatePicker(state = picker)
    }
}
