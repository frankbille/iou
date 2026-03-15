package dk.frankbille.iou.auth

internal expect class AuthTokenStorage() {
    fun load(): String?

    fun save(token: String)

    fun clear()
}
