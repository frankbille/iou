package dk.frankbille.iou.auth

import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.post
import dk.frankbille.iou.config.AppServerConfig
import okio.Buffer

internal enum class ParentAuthMode {
    LOGIN,
    REGISTER,
}

internal data class ParentAuthFormState(
    val mode: ParentAuthMode = ParentAuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

internal data class ParentAuthResponse(
    val accessToken: String,
    val parentName: String,
)

internal class ParentAuthException(
    override val message: String,
) : IllegalStateException(message)

internal class ParentAuthClient {
    private val httpEngine = DefaultHttpEngine()

    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): ParentAuthResponse =
        execute(
            url = AppServerConfig.parentRegisterUrl,
            body = encodeRegisterRequest(name = name.trim(), email = email.trim(), password = password),
            fallbackMessage = "Registration failed.",
            statusMessages =
                mapOf(
                    400 to "Enter a name, a valid email, and a password with at least 8 characters.",
                    409 to "A parent with this email already exists.",
                ),
        )

    suspend fun login(
        email: String,
        password: String,
    ): ParentAuthResponse =
        execute(
            url = AppServerConfig.parentLoginUrl,
            body = encodeLoginRequest(email = email.trim(), password = password),
            fallbackMessage = "Login failed.",
            statusMessages = mapOf(401 to "Incorrect email or password."),
        )

    private suspend fun execute(
        url: String,
        body: String,
        fallbackMessage: String,
        statusMessages: Map<Int, String>,
    ): ParentAuthResponse {
        val response =
            httpEngine
                .post(url)
                .addHeader("Content-Type", "application/json")
                .body(ByteStringHttpBody("application/json", body))
                .execute()

        val responseBody = response.body?.readUtf8().orEmpty()
        val statusMessage = statusMessages[response.statusCode]
        if (response.statusCode !in 200..299) {
            throw ParentAuthException(statusMessage ?: fallbackMessage)
        }

        return runCatching { decodeParentAuthResponse(responseBody) }
            .getOrElse {
                throw ParentAuthException("The server returned an unexpected authentication response.")
            }
    }
}

internal fun ParentAuthFormState.validatedForSubmit(): ParentAuthFormState {
    val trimmedName = name.trim()
    val trimmedEmail = email.trim()
    val error =
        when {
            mode == ParentAuthMode.REGISTER && trimmedName.isEmpty() -> "Enter your name."
            trimmedEmail.isEmpty() -> "Enter your email."
            password.isBlank() -> "Enter your password."
            mode == ParentAuthMode.REGISTER && password.length < 8 -> "Use at least 8 characters for the password."
            else -> null
        }

    return copy(
        name = trimmedName,
        email = trimmedEmail,
        error = error,
    )
}

private fun encodeRegisterRequest(
    name: String,
    email: String,
    password: String,
): String {
    val buffer = Buffer()
    val writer = BufferedSinkJsonWriter(buffer)
    writer.beginObject()
    writer.name("name")
    writer.value(name)
    writer.name("email")
    writer.value(email)
    writer.name("password")
    writer.value(password)
    writer.endObject()
    return buffer.readUtf8()
}

private fun encodeLoginRequest(
    email: String,
    password: String,
): String {
    val buffer = Buffer()
    val writer = BufferedSinkJsonWriter(buffer)
    writer.beginObject()
    writer.name("email")
    writer.value(email)
    writer.name("password")
    writer.value(password)
    writer.endObject()
    return buffer.readUtf8()
}

private fun decodeParentAuthResponse(responseBody: String): ParentAuthResponse {
    val reader = BufferedSourceJsonReader(Buffer().writeUtf8(responseBody))
    var accessToken: String? = null
    var parentName: String? = null

    reader.beginObject()
    while (reader.hasNext()) {
        when (reader.nextName()) {
            "accessToken" -> {
                accessToken = reader.nextString()
            }

            "parent" -> {
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name" -> parentName = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }

            else -> {
                reader.skipValue()
            }
        }
    }
    reader.endObject()

    return ParentAuthResponse(
        accessToken = requireNotNull(accessToken),
        parentName = requireNotNull(parentName),
    )
}
