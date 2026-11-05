package ru.littlehelper.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.littlehelper.app.R

// Шрифт Roboto Slab из макетов; файлы лежат в res/font
val RobotoSlab = FontFamily(
    Font(R.font.robotoslab_regular, FontWeight.Normal),
    Font(R.font.robotoslab_medium, FontWeight.Medium),
    Font(R.font.robotoslab_semibold, FontWeight.SemiBold),
    Font(R.font.robotoslab_bold, FontWeight.Bold),
)

private val base = Typography()

// Берём стандартные размеры Material и меняем только шрифт
val Typography = Typography(
    headlineMedium = base.headlineMedium.copy(fontFamily = RobotoSlab, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = RobotoSlab, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = RobotoSlab),
    titleMedium = base.titleMedium.copy(fontFamily = RobotoSlab, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = RobotoSlab),
    bodyMedium = base.bodyMedium.copy(fontFamily = RobotoSlab),
    bodySmall = base.bodySmall.copy(fontFamily = RobotoSlab),
    labelLarge = base.labelLarge.copy(fontFamily = RobotoSlab),
    labelMedium = base.labelMedium.copy(fontFamily = RobotoSlab),
)
