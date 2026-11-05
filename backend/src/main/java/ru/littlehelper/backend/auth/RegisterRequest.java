package ru.littlehelper.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.littlehelper.backend.user.Role;

public record RegisterRequest(
		@NotBlank(message = "Введите никнейм")
		@Pattern(regexp = "^[A-Za-z0-9_.]{3,30}$", message = "Никнейм: 3–30 символов, латиница, цифры, _ и .")
		String username,

		@NotBlank(message = "Введите имя")
		@Size(max = 100, message = "Слишком длинное имя")
		String fullName,

		@Pattern(regexp = "^(?=(?:\\D*\\d){10,15}\\D*$)[+0-9()\\s-]+$", message = "Некорректный номер телефона")
		String phone,

		@NotNull(message = "Выберите роль")
		Role role,

		@NotBlank(message = "Введите email")
		@Email(message = "Некорректный email")
		@Size(max = 100, message = "Слишком длинный email")
		String email,

		@NotBlank(message = "Введите пароль")
		@Size(min = 8, message = "Пароль: минимум 8 символов")
		@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "В пароле нужны и буквы, и цифры")
		String password
) {
}
