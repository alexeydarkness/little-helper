package ru.littlehelper.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.littlehelper.app.data.AppRepository
import ru.littlehelper.app.navigation.AppNavHost
import ru.littlehelper.app.ui.theme.LittleHelperTheme
import java.io.File

class ScheduleFlowTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun clearSession() {
        AppRepository.get(InstrumentationRegistry.getInstrumentation().targetContext).logout()
    }

    private fun login(email: String, name: String) {
        compose.setContent { LittleHelperTheme { AppNavHost() } }
        compose.onNodeWithText("Email").performTextInput(email)
        compose.onNodeWithText("Пароль").performTextInput("Demo12345")
        compose.onNodeWithText("Войти").performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(name, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun tutorCreatesAndDeletesLessonThroughNormalLogin() {
        login("tutor@demo.test", "Анна Петрова")
        compose.onAllNodesWithContentDescription("Добавить занятие").onFirst().performClick()
        compose.onNodeWithText("Ученик").performClick()
        compose.onNodeWithText("Саша Иванов · @demo_student").performClick()
        compose.onNodeWithText("Предмет").performTextInput("Проверка расписания")
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Проверка расписания").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("tutor-schedule.png")
        compose.onNodeWithText("Проверка расписания").performClick()
        compose.onNodeWithText("Удалить").performScrollTo().performClick()
        compose.onNodeWithText("Удалить занятие?").assertIsDisplayed()
        compose.onAllNodesWithText("Удалить").onLast().performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Проверка расписания").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun studentReadsHomeworkWithoutEditingControls() {
        login("student@demo.test", "Саша Иванов")
        compose.onAllNodesWithContentDescription("Добавить занятие").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Редактировать занятие").assertCountEquals(0)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Математика"))
        screenshot("student-schedule.png")
        compose.onNodeWithText("Математика").performClick()
        compose.onNodeWithText("Решить задачи 1–5").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").assertIsDisplayed()
        compose.onAllNodesWithText("Сохранить").assertCountEquals(0)
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
