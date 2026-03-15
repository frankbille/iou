package dk.frankbille.iou.auth

import dk.frankbille.iou.graphql.AndroidAppContextHolder
import java.io.File

private const val AUTH_TOKEN_FILENAME = "parent-auth.jwt"

internal actual class AuthTokenStorage {
    actual fun load(): String? =
        tokenFile()
            ?.takeIf(File::exists)
            ?.readText()
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    actual fun save(token: String) {
        tokenFile()?.writeText(token.trim())
    }

    actual fun clear() {
        tokenFile()?.delete()
    }

    private fun tokenFile(): File? =
        AndroidAppContextHolder.applicationContext
            ?.filesDir
            ?.resolve(AUTH_TOKEN_FILENAME)
}
