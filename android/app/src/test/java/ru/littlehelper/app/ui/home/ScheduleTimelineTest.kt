package ru.littlehelper.app.ui.home

import org.junit.Assert.*
import org.junit.Test
import ru.littlehelper.app.data.Lesson
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTimelineTest {
    private fun lesson(id: Long, start: String, end: String) = Lesson(
        id, 1, "Репетитор", 2, "Ученик", "Математика", LocalDate.of(2026, 12, 1),
        LocalTime.parse(start), LocalTime.parse(end), "", "", false,
    )

    @Test
    fun longLessonAppearsOnceAndHasNoFreeSlotsInside() {
        val lesson = lesson(1, "10:30", "12:15")
        val entries = scheduleEntries(listOf(lesson))
        assertEquals(1, entries.count { it.lesson != null })
        assertTrue(entries.filter { it.lesson == null }.none {
            it.start < lesson.endTime && it.end > lesson.startTime
        })
        assertTrue(entries.any { it.lesson == null && it.start == lesson.endTime })
    }

    @Test
    fun adjacentLessonsStayOrderedWithoutAnEmptySlotBetweenThem() {
        val first = lesson(1, "10:00", "11:00")
        val second = lesson(2, "11:00", "12:00")
        val entries = scheduleEntries(listOf(second, first))
        val index = entries.indexOfFirst { it.lesson?.id == 1L }
        assertEquals(2L, entries[index + 1].lesson?.id)
    }

    @Test
    fun emptyDayHasOnlyPositiveFreeIntervals() {
        val entries = scheduleEntries(emptyList())
        assertEquals(LocalTime.MIDNIGHT, entries.first().start)
        assertEquals(LocalTime.of(23, 59), entries.last().end)
        assertTrue(entries.all { it.lesson == null && it.end > it.start })
    }
}
