package ru.littlehelper.backend.user;

import java.time.Instant;

public record UserResponse(
		Long id,
		String username,
		String email,
		String fullName,
		String phone,
		Role role,
		int balance,
		Instant createdAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getFullName(),
				user.getPhone(),
				user.getRole(),
				user.getBalance(),
				user.getCreatedAt()
		);
	}
}
