package ru.littlehelper.backend.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.littlehelper.backend.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTests {

	private static final String PASSWORD = "secret123";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository users;

	private static String uniqueName() {
		return "u" + UUID.randomUUID().toString().substring(0, 8);
	}

	private ResultActions register(String username, String email, String password, String phone, String role) throws Exception {
		String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
		String body = """
				{"username":"%s","fullName":"Test User","phone":%s,"role":"%s","email":"%s","password":"%s"}
				""".formatted(username, phoneJson, role, email, password);
		return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions login(String email, String password) throws Exception {
		String body = """
				{"email":"%s","password":"%s"}
				""".formatted(email, password);
		return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void registerReturnsTokenAndUserWithoutPassword() throws Exception {
		String name = uniqueName();
		String email = name + "@test.com";

		register(name, email, PASSWORD, "+7 (930) 000-00-00", "TUTOR")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.username").value(name))
				.andExpect(jsonPath("$.user.role").value("TUTOR"))
				.andExpect(jsonPath("$.user.balance").value(0))
				.andExpect(jsonPath("$.user.password").doesNotExist())
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());

		String hash = users.findByEmail(email).orElseThrow().getPasswordHash();
		assertThat(hash).isNotEqualTo(PASSWORD).startsWith("$2");
	}

	@Test
	void phoneIsOptional() throws Exception {
		String name = uniqueName();
		register(name, name + "@test.com", PASSWORD, null, "STUDENT")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user.phone").doesNotExist());
	}

	@Test
	void duplicateEmailIsRejectedIgnoringCase() throws Exception {
		String name = uniqueName();
		register(name, name + "@test.com", PASSWORD, null, "STUDENT").andExpect(status().isCreated());

		register(uniqueName(), name.toUpperCase() + "@TEST.COM", PASSWORD, null, "STUDENT")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("email")));
	}

	@Test
	void duplicateUsernameIsRejectedIgnoringCase() throws Exception {
		String name = uniqueName();
		register(name, name + "@test.com", PASSWORD, null, "STUDENT").andExpect(status().isCreated());

		register(name.toUpperCase(), uniqueName() + "@test.com", PASSWORD, null, "STUDENT")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("никнейм")));
	}

	@Test
	void invalidFieldsAreRejected() throws Exception {
		String name = uniqueName();
		String email = name + "@test.com";

		register("ab", email, PASSWORD, null, "STUDENT").andExpect(status().isBadRequest());
		register(name, "not-an-email", PASSWORD, null, "STUDENT").andExpect(status().isBadRequest());
		register(name, email, "short1", null, "STUDENT").andExpect(status().isBadRequest());
		register(name, email, "12345678", null, "STUDENT")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("буквы")));
		register(name, email, PASSWORD, "12345", "STUDENT").andExpect(status().isBadRequest());
		register(name, email, PASSWORD, null, "ADMIN")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").isNotEmpty());
		register(name, email, "a1".repeat(40), null, "STUDENT").andExpect(status().isBadRequest());

		assertThat(users.existsByEmail(email)).isFalse();
	}

	@Test
	void loginWorksOnlyWithCorrectPassword() throws Exception {
		String name = uniqueName();
		String email = name + "@test.com";
		register(name, email, PASSWORD, null, "STUDENT").andExpect(status().isCreated());

		login(email, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
		login(email.toUpperCase(), PASSWORD).andExpect(status().isOk());

		login(email, "wrong-password1")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Неверный email или пароль"));
		login("nobody-" + email, PASSWORD)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Неверный email или пароль"));
		login(email, "x".repeat(100)).andExpect(status().isUnauthorized());
	}

	@Test
	void meRequiresValidToken() throws Exception {
		mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-token"))
				.andExpect(status().isUnauthorized());

		String name = uniqueName();
		String response = register(name, name + "@test.com", PASSWORD, null, "STUDENT")
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(response, "$.token");

		String forged = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";
		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + forged))
				.andExpect(status().isUnauthorized());

		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(name));
	}
}
