@file:Suppress("FunctionName")

package dk.frankbille.iou

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import dk.frankbille.iou.dashboard.HouseholdDashboard
import dk.frankbille.iou.dashboard.IouTheme
import dk.frankbille.iou.session.AppController
import dk.frankbille.iou.session.AppUiState
import dk.frankbille.iou.session.AuthScreen
import dk.frankbille.iou.session.LoadingScreen
import dk.frankbille.iou.session.NoFamiliesScreen
import dk.frankbille.iou.session.SessionErrorScreen
import dk.frankbille.iou.session.SessionSummaryCard
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val controller = remember { AppController() }

    LaunchedEffect(controller) {
        controller.initialize()
    }

    IouTheme {
        when (val state = controller.state) {
            is AppUiState.Auth -> {
                AuthScreen(
                    form = state.form,
                    onModeChange = controller::updateMode,
                    onNameChange = controller::updateName,
                    onEmailChange = controller::updateEmail,
                    onPasswordChange = controller::updatePassword,
                    onSubmit = {
                        scope.launch {
                            controller.submitAuth()
                        }
                    },
                )
            }

            is AppUiState.Loading -> {
                LoadingScreen(message = state.message)
            }

            is AppUiState.Dashboard -> {
                HouseholdDashboard(
                    state = state.state,
                    topContent = {
                        SessionSummaryCard(
                            viewerSummary = state.viewerSummary,
                            onLogout = controller::logout,
                        )
                    },
                )
            }

            is AppUiState.NoFamilies -> {
                NoFamiliesScreen(
                    viewerName = state.viewerName,
                    viewerSummary = state.viewerSummary,
                    onRetry = {
                        scope.launch {
                            controller.retryBootstrap()
                        }
                    },
                    onLogout = controller::logout,
                )
            }

            is AppUiState.SessionError -> {
                SessionErrorScreen(
                    message = state.message,
                    onRetry = {
                        scope.launch {
                            controller.retryBootstrap()
                        }
                    },
                    onLogout = controller::logout,
                )
            }
        }
    }
}
