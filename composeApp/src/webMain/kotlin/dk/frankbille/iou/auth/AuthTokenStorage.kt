package dk.frankbille.iou.auth

import kotlinx.browser.window

private const val AUTH_TOKEN_KEY = "iou.parent-auth.jwt"

internal actual class AuthTokenStorage {
    actual fun load(): String? =
        window.localStorage
            .getItem(AUTH_TOKEN_KEY)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    actual fun save(token: String) {
        window.localStorage.setItem(AUTH_TOKEN_KEY, token.trim())
    }

    actual fun clear() {
        window.localStorage.removeItem(AUTH_TOKEN_KEY)
    }
}
