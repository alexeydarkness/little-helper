package ru.littlehelper.backend.auth;

import ru.littlehelper.backend.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
