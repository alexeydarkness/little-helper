package ru.littlehelper.backend.lesson;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.littlehelper.backend.auth.JwtService;
import ru.littlehelper.backend.user.Role;
import ru.littlehelper.backend.user.User;
import ru.littlehelper.backend.user.UserRepository;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LessonApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService tokens;
    @Autowired PasswordEncoder passwords;

    private User account(Role role) {
        String name = "s" + UUID.randomUUID().toString().substring(0, 8);
        return users.saveAndFlush(new User(name, name + "@test.com", passwords.encode("Password123"), name, null, role));
    }

    private String body(Long student, String start, String end) {
        return """
                {"studentId":%d,"subject":"Математика","date":"2026-12-01",
                 "startTime":"%s","endTime":"%s","homework":"Задача 1","comment":"Повторение","paid":true}
                """.formatted(student, start, end);
    }

    private ResultActions create(User actor, Long student, String start, String end) throws Exception {
        return mvc.perform(post("/api/v1/lessons").header("Authorization", "Bearer " + tokens.createToken(actor))
                .contentType(MediaType.APPLICATION_JSON).content(body(student, start, end)));
    }

    @Test
    void tutorCreatesUpdatesAndDeletesLesson() throws Exception {
        User tutor = account(Role.TUTOR);
        User student = account(Role.STUDENT);
        String auth = "Bearer " + tokens.createToken(tutor);
        String response = create(tutor, student.getId(), "10:00", "11:00")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.paid").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mvc.perform(put("/api/v1/lessons/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(student.getId(), "12:00", "13:30")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.homework").value("Задача 1"));
        mvc.perform(get("/api/v1/lessons?date=2026-12-01").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(student.getId()));
        mvc.perform(delete("/api/v1/lessons/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/lessons?date=2026-12-01").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void studentSeesLessonsFromMultipleTutorsAndCannotWrite() throws Exception {
        User firstTutor = account(Role.TUTOR);
        User secondTutor = account(Role.TUTOR);
        User student = account(Role.STUDENT);
        User outsider = account(Role.STUDENT);
        String auth = "Bearer " + tokens.createToken(student);
        String response = create(firstTutor, student.getId(), "10:00", "11:00")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");
        create(secondTutor, student.getId(), "14:00", "15:00").andExpect(status().isCreated());
        mvc.perform(get("/api/v1/lessons?date=2026-12-01").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        create(student, outsider.getId(), "16:00", "17:00").andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/lessons/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(student.getId(), "12:00", "13:00")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/lessons/" + id).header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/lessons?date=2026-12-01")
                        .header("Authorization", "Bearer " + tokens.createToken(outsider)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anotherTutorCannotChangeLesson() throws Exception {
        User tutor = account(Role.TUTOR);
        User outsider = account(Role.TUTOR);
        User student = account(Role.STUDENT);
        String response = create(tutor, student.getId(), "10:00", "11:00")
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");
        String auth = "Bearer " + tokens.createToken(outsider);
        mvc.perform(put("/api/v1/lessons/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(student.getId(), "12:00", "13:00")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/lessons/" + id).header("Authorization", auth)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsOverlapsForEitherParticipantButAllowsAdjacentLessons() throws Exception {
        User tutor = account(Role.TUTOR);
        User secondTutor = account(Role.TUTOR);
        User student = account(Role.STUDENT);
        User secondStudent = account(Role.STUDENT);
        create(tutor, student.getId(), "10:00", "11:00").andExpect(status().isCreated());
        create(tutor, secondStudent.getId(), "10:30", "11:30").andExpect(status().isConflict());
        create(secondTutor, student.getId(), "09:30", "10:30").andExpect(status().isConflict());
        create(tutor, student.getId(), "11:00", "12:00").andExpect(status().isCreated());
    }

    @Test
    void validatesTimesParticipantsAndAuthentication() throws Exception {
        User tutor = account(Role.TUTOR);
        User student = account(Role.STUDENT);
        create(tutor, student.getId(), "12:00", "11:00").andExpect(status().isBadRequest());
        create(tutor, student.getId(), "12:00", "12:00").andExpect(status().isBadRequest());
        create(tutor, tutor.getId(), "10:00", "11:00").andExpect(status().isBadRequest());
        create(tutor, Long.MAX_VALUE, "10:00", "11:00").andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/lessons?date=2026-12-01")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/lessons?date=broken")
                        .header("Authorization", "Bearer " + tokens.createToken(tutor)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simultaneousBookingsForSameStudentCannotOverlap() throws Exception {
        User firstTutor = account(Role.TUTOR);
        User secondTutor = account(Role.TUTOR);
        User student = account(Role.STUDENT);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return create(firstTutor, student.getId(), "10:00", "11:00")
                        .andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return create(secondTutor, student.getId(), "10:00", "11:00")
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
    }
}
