package ru.littlehelper.backend.lesson;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {
    private final LessonService service;

    public LessonController(LessonService service) {
        this.service = service;
    }

    @GetMapping
    public List<LessonResponse> schedule(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.schedule(Long.valueOf(jwt.getSubject()), date);
    }

    @GetMapping("/students")
    public List<LessonService.Participant> students(@AuthenticationPrincipal Jwt jwt) {
        return service.students(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LessonRequest request) {
        return service.create(Long.valueOf(jwt.getSubject()), request);
    }

    @PutMapping("/{id}")
    public LessonResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody LessonRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(Long.valueOf(jwt.getSubject()), id);
    }
}
