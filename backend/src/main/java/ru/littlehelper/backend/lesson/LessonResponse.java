package ru.littlehelper.backend.lesson;

import java.time.LocalDate;
import java.time.LocalTime;

public record LessonResponse(
        Long id, Long tutorId, String tutorName, Long studentId, String studentName,
        String subject, LocalDate date, LocalTime startTime, LocalTime endTime,
        String homework, String comment, boolean paid
) {
    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(), lesson.getTutor().getId(), lesson.getTutor().getFullName(),
                lesson.getStudent().getId(), lesson.getStudent().getFullName(),
                lesson.getSubject(), lesson.getDate(), lesson.getStartTime(), lesson.getEndTime(),
                lesson.getHomework(), lesson.getComment(), lesson.isPaid()
        );
    }
}
