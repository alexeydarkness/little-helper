package ru.littlehelper.backend.user;

public record UserSearchResult(Long id, String fullName, String username, Role role) {
}
