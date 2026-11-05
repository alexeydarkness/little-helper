package ru.littlehelper.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "Введите email")
		String email,

		@NotBlank(message = "Введите пароль")
		String password
) {
}
