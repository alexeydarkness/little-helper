package ru.littlehelper.backend.lesson;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.littlehelper.backend.user.Role;
import ru.littlehelper.backend.user.User;
import ru.littlehelper.backend.user.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@Profile("dev")
public class DemoData implements CommandLineRunner {
    private final UserRepository users;
    private final LessonRepository lessons;
    private final PasswordEncoder passwords;

    public DemoData(UserRepository users, LessonRepository lessons, PasswordEncoder passwords) {
        this.users = users;
        this.lessons = lessons;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByEmail("tutor@demo.test")) return;
        User tutor = account("demo_tutor", "tutor@demo.test", "Анна Петрова", Role.TUTOR);
        User secondTutor = account("demo_tutor2", "tutor2@demo.test", "Михаил Орлов", Role.TUTOR);
        User student = account("demo_student", "student@demo.test", "Саша Иванов", Role.STUDENT);
        User secondStudent = account("demo_student2", "student2@demo.test", "Маша Смирнова", Role.STUDENT);
        LocalDate today = LocalDate.now();
        lessons.save(new Lesson(tutor, student, new LessonRequest(student.getId(), "Математика", today,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Решить задачи 1–5", "Повторим дроби", true)));
        lessons.save(new Lesson(tutor, secondStudent, new LessonRequest(secondStudent.getId(), "Русский язык", today,
                LocalTime.of(14, 0), LocalTime.of(15, 0), "Упражнение 24", null, false)));
        lessons.save(new Lesson(secondTutor, student, new LessonRequest(student.getId(), "Английский язык", today,
                LocalTime.of(16, 0), LocalTime.of(17, 0), "Выучить 10 слов", null, false)));
    }

    private User account(String username, String email, String name, Role role) {
        return users.save(new User(username, email, passwords.encode("Demo12345"), name, null, role));
    }
}
