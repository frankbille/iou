package dk.frankbille.iou.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dk.frankbille.iou.auth.AuthTokenStorage
import dk.frankbille.iou.auth.ParentAuthClient
import dk.frankbille.iou.auth.ParentAuthException
import dk.frankbille.iou.auth.ParentAuthFormState
import dk.frankbille.iou.auth.ParentAuthMode
import dk.frankbille.iou.auth.validatedForSubmit
import dk.frankbille.iou.dashboard.DashboardState
import dk.frankbille.iou.graphql.DashboardDataResult
import dk.frankbille.iou.graphql.DashboardRepository
import dk.frankbille.iou.graphql.GraphqlSessionStore
import dk.frankbille.iou.graphql.NoFamiliesDashboardResult

internal sealed interface AppUiState {
    data class Auth(
        val form: ParentAuthFormState = ParentAuthFormState(),
    ) : AppUiState

    data class Loading(
        val message: String,
    ) : AppUiState

    data class Dashboard(
        val state: DashboardState,
        val viewerSummary: String,
    ) : AppUiState

    data class NoFamilies(
        val viewerName: String,
        val viewerSummary: String,
    ) : AppUiState

    data class SessionError(
        val message: String,
    ) : AppUiState
}

internal class AppController(
    private val authTokenStorage: AuthTokenStorage = AuthTokenStorage(),
    private val sessionStore: GraphqlSessionStore = GraphqlSessionStore(),
    private val dashboardRepository: DashboardRepository = DashboardRepository(sessionStore = sessionStore),
    private val parentAuthClient: ParentAuthClient = ParentAuthClient(),
) {
    var state by mutableStateOf<AppUiState>(AppUiState.Loading(message = "Checking for a saved session..."))
        private set

    private var initialized = false

    suspend fun initialize() {
        if (initialized) {
            return
        }
        initialized = true

        val storedToken = authTokenStorage.load()?.trim().takeIf { !it.isNullOrEmpty() }
        if (storedToken == null) {
            state = AppUiState.Auth()
            return
        }

        sessionStore.jwt = storedToken
        bootstrapDashboard(loadingMessage = "Loading your household...")
    }

    fun updateMode(mode: ParentAuthMode) {
        val authState = state as? AppUiState.Auth ?: return
        state =
            authState.copy(
                form =
                    authState.form.copy(
                        mode = mode,
                        error = null,
                    ),
            )
    }

    fun updateName(name: String) {
        val authState = state as? AppUiState.Auth ?: return
        state = authState.copy(form = authState.form.copy(name = name, error = null))
    }

    fun updateEmail(email: String) {
        val authState = state as? AppUiState.Auth ?: return
        state = authState.copy(form = authState.form.copy(email = email, error = null))
    }

    fun updatePassword(password: String) {
        val authState = state as? AppUiState.Auth ?: return
        state = authState.copy(form = authState.form.copy(password = password, error = null))
    }

    suspend fun submitAuth() {
        val authState = state as? AppUiState.Auth ?: return
        val validatedForm = authState.form.validatedForSubmit()
        if (validatedForm.error != null) {
            state = authState.copy(form = validatedForm)
            return
        }

        state = authState.copy(form = validatedForm.copy(isSubmitting = true, error = null))

        try {
            val authResponse =
                when (validatedForm.mode) {
                    ParentAuthMode.LOGIN -> {
                        parentAuthClient.login(
                            email = validatedForm.email,
                            password = validatedForm.password,
                        )
                    }

                    ParentAuthMode.REGISTER -> {
                        parentAuthClient.register(
                            name = validatedForm.name,
                            email = validatedForm.email,
                            password = validatedForm.password,
                        )
                    }
                }

            authTokenStorage.save(authResponse.accessToken)
            sessionStore.jwt = authResponse.accessToken
            bootstrapDashboard(loadingMessage = "Loading ${authResponse.parentName}'s household...")
        } catch (exception: ParentAuthException) {
            state =
                AppUiState.Auth(
                    form =
                        validatedForm.copy(
                            isSubmitting = false,
                            error = exception.message,
                        ),
                )
        }
    }

    suspend fun retryBootstrap() {
        if (sessionStore.bearerTokenOrNull() == null) {
            state = AppUiState.Auth()
            return
        }

        bootstrapDashboard(loadingMessage = "Retrying your household load...")
    }

    fun logout() {
        authTokenStorage.clear()
        dashboardRepository.clearSnapshot()
        sessionStore.jwt = ""
        state = AppUiState.Auth()
    }

    private suspend fun bootstrapDashboard(loadingMessage: String) {
        state = AppUiState.Loading(message = loadingMessage)

        state =
            try {
                when (val result = dashboardRepository.refreshDashboard()) {
                    is DashboardDataResult -> {
                        AppUiState.Dashboard(
                            state = result.dashboardState,
                            viewerSummary = result.viewerSummary,
                        )
                    }

                    is NoFamiliesDashboardResult -> {
                        AppUiState.NoFamilies(
                            viewerName = result.viewerName,
                            viewerSummary = result.viewerSummary,
                        )
                    }
                }
            } catch (exception: IllegalStateException) {
                AppUiState.SessionError(
                    message = exception.message ?: "Unable to load the household dashboard.",
                )
            }
    }
}
