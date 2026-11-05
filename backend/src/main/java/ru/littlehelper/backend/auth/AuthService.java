package ru.littlehelper.backend.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.littlehelper.backend.common.ApiException;
import ru.littlehelper.backend.user.User;
import ru.littlehelper.backend.user.UserRepository;
import ru.littlehelper.backend.user.UserResponse;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

	private static final int BCRYPT_MAX_BYTES = 72;

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse register(RegisterRequest request) {
		if (tooLongForBcrypt(request.password())) {
			throw ApiException.badRequest("Слишком длинный пароль");
		}
		String email = normalizeEmail(request.email());
		String username = request.username().trim();

		if (users.existsByEmail(email)) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}
		if (users.existsByUsernameIgnoreCase(username)) {
			throw ApiException.conflict("Этот никнейм уже занят");
		}

		User user = new User(
				username,
				email,
				passwordEncoder.encode(request.password()),
				request.fullName().trim(),
				request.phone() == null ? null : request.phone().trim(),
				request.role()
		);
		try {
			return toResponse(users.saveAndFlush(user));
		} catch (DataIntegrityViolationException e) {
			throw ApiException.conflict("Пользователь с такими данными уже существует");
		}
	}

	public AuthResponse login(LoginRequest request) {
		ApiException invalid = ApiException.unauthorized("Неверный email или пароль");

		if (tooLongForBcrypt(request.password())) {
			throw invalid;
		}
		User user = users.findByEmail(normalizeEmail(request.email())).orElseThrow(() -> invalid);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalid;
		}
		return toResponse(user);
	}

	private AuthResponse toResponse(User user) {
		return new AuthResponse(jwtService.createToken(user), UserResponse.from(user));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean tooLongForBcrypt(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
	}
}
