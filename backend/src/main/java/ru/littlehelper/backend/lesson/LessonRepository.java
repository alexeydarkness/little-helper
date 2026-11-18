package ru.littlehelper.backend.lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @Query("""
            select l from Lesson l join fetch l.tutor join fetch l.student
            where l.date = :date and (l.tutor.id = :userId or l.student.id = :userId)
            order by l.startTime, l.id
            """)
    List<Lesson> findSchedule(Long userId, LocalDate date);

    Optional<Lesson> findByIdAndTutorId(Long id, Long tutorId);

    @Query("""
            select count(l) > 0 from Lesson l
            where l.date = :date and l.startTime < :endTime and l.endTime > :startTime
            and (l.tutor.id = :tutorId or l.student.id = :studentId)
            and (:excludedId is null or l.id <> :excludedId)
            """)
    boolean overlaps(Long tutorId, Long studentId, LocalDate date,
                     LocalTime startTime, LocalTime endTime, Long excludedId);
}
