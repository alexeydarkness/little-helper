package ru.littlehelper.backend.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.]{3,30}$", message = "Никнейм: 3–30 символов, латиница, цифры, _ и .")
        String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 100) String fullName,
        @Pattern(regexp = "^(?=(?:\\D*\\d){10,15}\\D*$)[+0-9()\\s-]+$", message = "Некорректный номер телефона")
        String phone,
        @Size(max = 400000, message = "Слишком большое фото") String avatarBase64
) {
}
