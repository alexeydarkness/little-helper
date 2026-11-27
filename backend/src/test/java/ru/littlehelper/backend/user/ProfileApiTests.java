package ru.littlehelper.backend.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.littlehelper.backend.auth.JwtService;

import java.util.UUID;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService tokens;
    @Autowired PasswordEncoder passwords;

    private User account() {
        String name = "p" + UUID.randomUUID().toString().substring(0, 8);
        return users.saveAndFlush(new User(name, name + "@test.com", passwords.encode("Password123"),
                "Test User", null, Role.STUDENT));
    }

    private String body(String username, String email, String avatar) {
        return """
                {"username":"%s","email":"%s","fullName":"Новое имя","phone":null,"avatarBase64":%s}
                """.formatted(username, email, avatar);
    }

    @Test
    void updatesOnlyAuthenticatedUserAndKeepsExistingTokenValid() throws Exception {
        User user = account();
        User other = account();
        String auth = "Bearer " + tokens.createToken(user);
        String name = "new" + user.getUsername();
        mvc.perform(put("/api/v1/users/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, name + "@TEST.COM", "null")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(name + "@test.com"))
                .andExpect(jsonPath("$.role").value("STUDENT")).andExpect(jsonPath("$.balance").value(0));
        mvc.perform(get("/api/v1/users/me").header("Authorization", auth))
                .andExpect(jsonPath("$.username").value(name));
        assertThat(users.findById(other.getId()).orElseThrow().getFullName()).isEqualTo("Test User");
    }

    @Test
    void rejectsTakenNamesEmailsInvalidPhotosAndAnonymousUpdates() throws Exception {
        User user = account();
        User other = account();
        String auth = "Bearer " + tokens.createToken(user);
        mvc.perform(put("/api/v1/users/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(other.getUsername().toUpperCase(), user.getEmail(), "null")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/users/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(user.getUsername(), other.getEmail(), "null")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/users/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(user.getUsername(), user.getEmail(), "\"invalid image\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user.getUsername(), user.getEmail(), "null")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void avatarCanBeSavedKeptAndRemoved() throws Exception {
        User user = account();
        String auth = "Bearer " + tokens.createToken(user);
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", output);
        String avatar = Base64.getEncoder().encodeToString(output.toByteArray());
        for (String value : new String[]{"\"" + avatar + "\"", "null"}) {
            mvc.perform(put("/api/v1/users/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content(body(user.getUsername(), user.getEmail(), value)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.avatarBase64").value(avatar));
        }
        mvc.perform(put("/api/v1/users/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(body(user.getUsername(), user.getEmail(), "\"\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarBase64").doesNotExist());
    }

    @Test
    void simultaneousRenamesCannotCreateCaseInsensitiveDuplicates() throws Exception {
        User firstUser = account();
        User secondUser = account();
        String name = "shared" + UUID.randomUUID().toString().substring(0, 8);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return mvc.perform(put("/api/v1/users/me")
                                .header("Authorization", "Bearer " + tokens.createToken(firstUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(name, firstUser.getEmail(), "null")))
                        .andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return mvc.perform(put("/api/v1/users/me")
                                .header("Authorization", "Bearer " + tokens.createToken(secondUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(name.toUpperCase(), secondUser.getEmail(), "null")))
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
    }
}
