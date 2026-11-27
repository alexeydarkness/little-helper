package ru.littlehelper.backend.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.littlehelper.backend.auth.JwtService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserSearchApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService tokens;

    @Test
    void searchesOppositeRolesByNameAndReturnsOnlyPublicFields() throws Exception {
        String term = uniqueTerm();
        User tutor = account(term + "t", "Анна " + term, Role.TUTOR);
        User student = account(term + "s", "Саша " + term, Role.STUDENT);

        mvc.perform(search(tutor, "сАшА " + term))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(student.getId()))
                .andExpect(jsonPath("$[0].fullName").value(student.getFullName()))
                .andExpect(jsonPath("$[0].username").value(student.getUsername()))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].balance").doesNotExist())
                .andExpect(jsonPath("$[0].avatarBase64").doesNotExist());
        mvc.perform(search(student, term))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tutor.getId()))
                .andExpect(jsonPath("$[0].role").value("TUTOR"));
    }

    @Test
    void trimsNicknameSearchAndTreatsSqlPatternCharactersLiterally() throws Exception {
        String term = uniqueTerm();
        User tutor = account(term + "t", "Репетитор", Role.TUTOR);
        User student = account(term + "_user", "Саша " + term, Role.STUDENT);
        account(term + "xuser", "Петя " + term, Role.STUDENT);

        mvc.perform(search(tutor, "  @ " + term.toUpperCase(java.util.Locale.ROOT) + "_US  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(student.getId()));
        mvc.perform(search(tutor, term + "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void validatesQueryAndRequiresAuthentication() throws Exception {
        String term = uniqueTerm();
        User tutor = account(term + "t", "Репетитор", Role.TUTOR);
        mvc.perform(get("/api/v1/users/search").param("q", term)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/search").header("Authorization", "Bearer " + tokens.createToken(tutor)))
                .andExpect(status().isBadRequest());
        mvc.perform(search(tutor, " @ x ")).andExpect(status().isBadRequest());
        mvc.perform(search(tutor, "a".repeat(101))).andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder search(User user, String term) {
        return get("/api/v1/users/search").param("q", term)
                .header("Authorization", "Bearer " + tokens.createToken(user));
    }

    private User account(String username, String fullName, Role role) {
        return users.saveAndFlush(new User(username, username + "@test.com", "unused", fullName, null, role));
    }

    private String uniqueTerm() {
        return "search" + UUID.randomUUID().toString().substring(0, 8);
    }
}
