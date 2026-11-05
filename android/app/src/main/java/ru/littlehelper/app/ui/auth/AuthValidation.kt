package ru.littlehelper.app.ui.auth

/** Проверки полей входа и регистрации. Каждая функция возвращает текст ошибки или null, если всё верно. */
object AuthValidation {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val usernameRegex = Regex("^[A-Za-z0-9_.]{3,30}$")
    private val nonDigits = Regex("\\D")

    const val MIN_PASSWORD_LENGTH = 8

    fun email(value: String): String? = when {
        value.isBlank() -> "Введите email"
        !emailRegex.matches(value.trim()) -> "Некорректный email"
        else -> null
    }

    /** Для входа достаточно, чтобы пароль был не пустым. */
    fun loginPassword(value: String): String? =
        if (value.isEmpty()) "Введите пароль" else null

    /** Для регистрации: минимум 8 символов, буквы и цифры. */
    fun newPassword(value: String): String? = when {
        value.length < MIN_PASSWORD_LENGTH -> "Минимум $MIN_PASSWORD_LENGTH символов"
        value.none { it.isDigit() } || value.none { it.isLetter() } -> "Нужны и буквы, и цифры"
        else -> null
    }

    fun username(value: String): String? = when {
        value.isBlank() -> "Введите никнейм"
        !usernameRegex.matches(value.trim()) -> "3–30 символов: латиница, цифры, _ и ."
        else -> null
    }

    fun fullName(value: String): String? = when {
        value.isBlank() -> "Введите имя"
        value.trim().length > 100 -> "Слишком длинное имя"
        else -> null
    }

    /** Телефон необязателен; если указан — от 10 до 15 цифр. */
    fun phone(value: String): String? {
        if (value.isBlank()) return null
        val digits = value.replace(nonDigits, "")
        return if (digits.length in 10..15) null else "Некорректный номер телефона"
    }
}
