package ru.littlehelper.backend.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.littlehelper.backend.common.ApiException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserRepository users;
	private final ProfileService profiles;

	public UserController(UserRepository users, ProfileService profiles) {
		this.users = users;
		this.profiles = profiles;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		Long id = Long.valueOf(jwt.getSubject());
		return users.findById(id)
				.map(UserResponse::from)
				.orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
	}

	@PutMapping("/me")
	public UserResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
		return profiles.update(Long.valueOf(jwt.getSubject()), request);
	}
}
