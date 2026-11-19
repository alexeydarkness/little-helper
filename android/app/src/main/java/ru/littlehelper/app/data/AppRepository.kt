package ru.littlehelper.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.littlehelper.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime

data class AppUser(val id: Long, val fullName: String, val role: String) {
    val isTutor: Boolean get() = role == "TUTOR"
}

data class Student(val id: Long, val fullName: String, val username: String)

data class Lesson(
    val id: Long,
    val tutorId: Long,
    val tutorName: String,
    val studentId: Long,
    val studentName: String,
    val subject: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val homework: String,
    val comment: String,
    val paid: Boolean,
) {
    fun participantName(user: AppUser): String = if (user.isTutor) studentName else tutorName
    fun participantId(user: AppUser): Long = if (user.isTutor) studentId else tutorId
}

data class LessonDraft(
    val id: Long? = null,
    val studentId: Long? = null,
    val subject: String = "",
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val homework: String = "",
    val comment: String = "",
    val paid: Boolean = false,
)

data class UserProfile(
    val username: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val balance: Int,
    val createdAt: String,
    val avatarBase64: String,
)

class ApiFailure(message: String, val unauthorized: Boolean = false) : Exception(message)

class AppRepository private constructor(context: Context) {
    private val session = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun hasSession(): Boolean = session.contains("token")

    fun logout() {
        session.edit { clear() }
    }

    suspend fun login(email: String, password: String) {
        val response = JSONObject(request("POST", "/auth/login", JSONObject()
            .put("email", email.trim()).put("password", password), authenticated = false))
        session.edit { putString("token", response.getString("token")) }
    }

    suspend fun register(username: String, fullName: String, phone: String, role: String,
                         email: String, password: String) {
        val body = JSONObject().put("username", username.trim()).put("fullName", fullName.trim())
            .put("phone", phone.trim().ifEmpty { null } ?: JSONObject.NULL).put("role", role)
            .put("email", email.trim()).put("password", password)
        val response = JSONObject(request("POST", "/auth/register", body, authenticated = false))
        session.edit { putString("token", response.getString("token")) }
    }

    suspend fun currentUser(): AppUser {
        val user = JSONObject(request("GET", "/users/me"))
        return AppUser(user.getLong("id"), user.getString("fullName"), user.getString("role"))
    }

    suspend fun profile(): UserProfile = parseProfile(JSONObject(request("GET", "/users/me")))

    suspend fun updateProfile(profile: UserProfile): UserProfile {
        val body = JSONObject().put("username", profile.username.trim())
            .put("email", profile.email.trim()).put("fullName", profile.fullName.trim())
            .put("phone", profile.phone.trim().ifEmpty { null } ?: JSONObject.NULL)
            .put("avatarBase64", profile.avatarBase64)
        return parseProfile(JSONObject(request("PUT", "/users/me", body)))
    }

    private fun parseProfile(value: JSONObject) = UserProfile(
        value.getString("username"), value.getString("email"), value.getString("fullName"),
        value.optionalText("phone"), value.getString("role"), value.getInt("balance"),
        value.getString("createdAt"), value.optionalText("avatarBase64"),
    )

    suspend fun students(): List<Student> {
        val array = JSONArray(request("GET", "/lessons/students"))
        return List(array.length()) { index ->
            val user = array.getJSONObject(index)
            Student(user.getLong("id"), user.getString("fullName"), user.getString("username"))
        }
    }

    suspend fun lessons(date: LocalDate): List<Lesson> {
        val array = JSONArray(request("GET", "/lessons?date=$date"))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Lesson(item.getLong("id"), item.getLong("tutorId"), item.getString("tutorName"),
                item.getLong("studentId"), item.getString("studentName"), item.getString("subject"),
                LocalDate.parse(item.getString("date")), LocalTime.parse(item.getString("startTime")),
                LocalTime.parse(item.getString("endTime")), item.optionalText("homework"),
                item.optionalText("comment"), item.getBoolean("paid"))
        }
    }

    suspend fun saveLesson(draft: LessonDraft) {
        val body = JSONObject().put("studentId", draft.studentId).put("subject", draft.subject.trim())
            .put("date", draft.date.toString()).put("startTime", draft.startTime.toString())
            .put("endTime", draft.endTime.toString()).put("homework", draft.homework.trim())
            .put("comment", draft.comment.trim()).put("paid", draft.paid)
        if (draft.id == null) {
            request("POST", "/lessons", body)
        } else {
            request("PUT", "/lessons/${draft.id}", body)
        }
    }

    suspend fun deleteLesson(id: Long) {
        request("DELETE", "/lessons/$id")
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null,
                                authenticated: Boolean = true): String = withContext(Dispatchers.IO) {
        val connection = URL("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/v1$path")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (authenticated) {
                val token = session.getString("token", null)
                    ?: throw ApiFailure("Войдите в аккаунт", unauthorized = true)
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(response).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "Не удалось выполнить запрос. Повторите попытку."
                throw ApiFailure(message, unauthorized = status == 401)
            }
            response
        } catch (error: IOException) {
            throw ApiFailure("Нет соединения с сервером. Повторите попытку.")
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optionalText(key: String): String = if (isNull(key)) "" else getString(key)

    companion object {
        @Volatile private var instance: AppRepository? = null

        fun get(context: Context): AppRepository = instance ?: synchronized(this) {
            instance ?: AppRepository(context.applicationContext).also { instance = it }
        }
    }
}
