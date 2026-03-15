package dk.frankbille.iou.auth

import platform.Foundation.NSUserDefaults

private const val AUTH_TOKEN_KEY = "parent-auth.jwt"

internal actual class AuthTokenStorage {
    actual fun load(): String? =
        NSUserDefaults.standardUserDefaults
            .stringForKey(AUTH_TOKEN_KEY)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    actual fun save(token: String) {
        NSUserDefaults.standardUserDefaults.setObject(token.trim(), AUTH_TOKEN_KEY)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(AUTH_TOKEN_KEY)
    }
}
