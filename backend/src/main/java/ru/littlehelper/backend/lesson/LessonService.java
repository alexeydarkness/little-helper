package ru.littlehelper.backend.lesson;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.littlehelper.backend.common.ApiException;
import ru.littlehelper.backend.user.Role;
import ru.littlehelper.backend.user.User;
import ru.littlehelper.backend.user.UserRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LessonService {
    private final LessonRepository lessons;
    private final UserRepository users;

    public LessonService(LessonRepository lessons, UserRepository users) {
        this.lessons = lessons;
        this.users = users;
    }

    public List<LessonResponse> schedule(Long userId, LocalDate date) {
        user(userId);
        return lessons.findSchedule(userId, date).stream().map(LessonResponse::from).toList();
    }

    public record Participant(Long id, String fullName, String username) {
    }

    public List<Participant> students(Long userId) {
        requireTutor(user(userId));
        return users.findByRoleOrderByFullNameAsc(Role.STUDENT).stream()
                .map(u -> new Participant(u.getId(), u.getFullName(), u.getUsername())).toList();
    }

    @Transactional
    public LessonResponse create(Long userId, LessonRequest request) {
        User tutor = lockTutor(userId);
        User student = validate(tutor, request, null);
        return LessonResponse.from(lessons.save(new Lesson(tutor, student, request)));
    }

    @Transactional
    public LessonResponse update(Long userId, Long lessonId, LessonRequest request) {
        User tutor = lockTutor(userId);
        Lesson lesson = ownedLesson(lessonId, tutor.getId());
        User student = validate(tutor, request, lessonId);
        lesson.update(student, request);
        return LessonResponse.from(lesson);
    }

    @Transactional
    public void delete(Long userId, Long lessonId) {
        User tutor = lockTutor(userId);
        lessons.delete(ownedLesson(lessonId, tutor.getId()));
    }

    private User validate(User tutor, LessonRequest request, Long excludedId) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw ApiException.badRequest("Конец занятия должен быть позже начала в пределах выбранного дня");
        }
        User student = users.findByIdForUpdate(request.studentId())
                .orElseThrow(() -> ApiException.badRequest("Ученик не найден"));
        if (student.getRole() != Role.STUDENT) {
            throw ApiException.badRequest("Выберите пользователя с ролью ученика");
        }
        if (lessons.overlaps(tutor.getId(), student.getId(), request.date(),
                request.startTime(), request.endTime(), excludedId)) {
            throw ApiException.conflict("Это время уже занято у репетитора или ученика");
        }
        return student;
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
    }

    private User lockTutor(Long id) {
        User user = users.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
        requireTutor(user);
        return user;
    }

    private void requireTutor(User user) {
        if (user.getRole() != Role.TUTOR) {
            throw ApiException.forbidden("Только репетитор может изменять расписание");
        }
    }

    private Lesson ownedLesson(Long id, Long tutorId) {
        return lessons.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> ApiException.notFound("Занятие не найдено"));
    }
}
