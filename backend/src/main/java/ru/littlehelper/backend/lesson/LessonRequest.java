package ru.littlehelper.backend.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record LessonRequest(
        @NotNull(message = "Выберите ученика") @Positive Long studentId,
        @NotBlank(message = "Введите предмет") @Size(max = 100) String subject,
        @NotNull(message = "Выберите дату") LocalDate date,
        @NotNull(message = "Укажите начало занятия") LocalTime startTime,
        @NotNull(message = "Укажите конец занятия") LocalTime endTime,
        @Size(max = 2000, message = "Домашнее задание: максимум 2000 символов") String homework,
        @Size(max = 2000, message = "Комментарий: максимум 2000 символов") String comment,
        boolean paid
) {
}
