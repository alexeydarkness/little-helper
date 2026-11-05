package ru.littlehelper.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Одна светлая зелёная схема, как на макетах. Тёмная тема — позже, в задаче «Настройки»
private val LightColors = lightColorScheme(
    primary = GreenButton,
    onPrimary = Color.White,
    secondary = GreenTopBar,
    onSecondary = GreenCard,
    background = GreenPage,
    onBackground = GreenText,
    surface = GreenCard,
    onSurface = GreenText,
    surfaceVariant = GreenSlot,
    onSurfaceVariant = GreenText,
    outline = GreyOutline,
    error = ErrorRed,
)

@Composable
fun LittleHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
