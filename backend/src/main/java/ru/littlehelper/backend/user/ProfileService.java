package ru.littlehelper.backend.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.littlehelper.backend.common.ApiException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

@Service
public class ProfileService {
    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public UserResponse update(Long userId, UpdateProfileRequest request) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCaseAndIdNot(username, userId)) {
            throw ApiException.conflict("Этот никнейм уже занят");
        }
        if (users.existsByEmailAndIdNot(email, userId)) {
            throw ApiException.conflict("Этот email уже используется");
        }
        validateAvatar(request.avatarBase64());
        user.updateProfile(username, email, request.fullName().trim(), request.phone(), request.avatarBase64());
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException error) {
            throw ApiException.conflict("Никнейм или email уже используется");
        }
    }

    private void validateAvatar(String avatar) {
        if (avatar == null || avatar.isEmpty()) return;
        try {
            byte[] bytes = Base64.getDecoder().decode(avatar);
            if (bytes.length > 300000) throw ApiException.badRequest("Слишком большое фото");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw ApiException.badRequest("Выберите фото JPEG или PNG");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("JPEG") || format.equalsIgnoreCase("PNG"))
                            || reader.getWidth(0) > 1024 || reader.getHeight(0) > 1024) {
                        throw ApiException.badRequest("Фото должно быть JPEG или PNG размером до 1024×1024");
                    }
                    reader.read(0);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException error) {
            throw ApiException.badRequest("Не удалось прочитать фото");
        }
    }
}
