package ru.littlehelper.backend.lesson;

import jakarta.persistence.*;
import ru.littlehelper.backend.user.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private User tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 100)
    private String subject;
    @Column(name = "lesson_date", nullable = false)
    private LocalDate date;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(length = 2000)
    private String homework;
    @Column(length = 2000)
    private String comment;
    @Column(nullable = false)
    private boolean paid;

    protected Lesson() {
    }

    public Lesson(User tutor, User student, LessonRequest request) {
        this.tutor = tutor;
        update(student, request);
    }

    public void update(User student, LessonRequest request) {
        this.student = student;
        subject = request.subject().trim();
        date = request.date();
        startTime = request.startTime();
        endTime = request.endTime();
        homework = request.homework();
        comment = request.comment();
        paid = request.paid();
    }

    public Long getId() { return id; }
    public User getTutor() { return tutor; }
    public User getStudent() { return student; }
    public String getSubject() { return subject; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getHomework() { return homework; }
    public String getComment() { return comment; }
    public boolean isPaid() { return paid; }
}
