package dk.frankbille.iou.config

internal object AppServerConfig {
    // Keep the backend address in app config instead of user-entered UI state.
    private const val SERVER_BASE_URL = "http://localhost:8080"

    private val normalizedBaseUrl = SERVER_BASE_URL.removeSuffix("/")

    val graphqlUrl: String = "$normalizedBaseUrl/graphql"
    val parentLoginUrl: String = "$normalizedBaseUrl/auth/parents/login"
    val parentRegisterUrl: String = "$normalizedBaseUrl/auth/parents/register"
}
