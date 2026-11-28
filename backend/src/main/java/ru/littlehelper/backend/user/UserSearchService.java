package ru.littlehelper.backend.user;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.littlehelper.backend.common.ApiException;

import java.util.List;

@Service
public class UserSearchService {
    private static final int MAX_RESULTS = 20;
    private final UserRepository users;

    public UserSearchService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResult> findUsers(Long userId, String query) {
        String term = query.strip();
        if (term.startsWith("@")) term = term.substring(1).strip();
        int length = term.codePointCount(0, term.length());
        if (length < 2 || length > 100) {
            throw ApiException.badRequest("Запрос должен содержать от 2 до 100 символов");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
        Role targetRole = user.getRole() == Role.TUTOR ? Role.STUDENT : Role.TUTOR;
        return users.searchByRoleAndTerm(targetRole, term, PageRequest.of(0, MAX_RESULTS));
    }
}
