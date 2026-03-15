@file:Suppress("FunctionName")

package dk.frankbille.iou.graphql

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

internal data class GraphqlDashboardState(
    val dashboardStateLoaded: Boolean = false,
    val viewerSummary: String? = null,
    val status: String = "JWT + Apollo snapshot POC. Load a broader family payload and map it into the dashboard cards.",
    val isLoading: Boolean = false,
    val error: String? = null,
)

internal class GraphqlDashboardController(
    val sessionStore: GraphqlSessionStore = GraphqlSessionStore(),
) {
    private val repository = DashboardRepository(sessionStore = sessionStore)

    var loadedDashboardState by mutableStateOf<dk.frankbille.iou.dashboard.DashboardState?>(null)
        private set

    var state by mutableStateOf(GraphqlDashboardState())
        private set

    suspend fun loadCacheFirst() {
        runRequest { repository.loadDashboard() }
    }

    suspend fun readCacheOnly() {
        runRequest { repository.readCachedDashboard() }
    }

    suspend fun refreshFromNetwork() {
        runRequest { repository.refreshDashboard() }
    }

    private suspend fun runRequest(block: suspend () -> DashboardDataResult) {
        state = state.copy(isLoading = true, error = null)

        state =
            try {
                val result = block()
                loadedDashboardState = result.dashboardState
                GraphqlDashboardState(
                    dashboardStateLoaded = true,
                    viewerSummary = result.viewerSummary,
                    status =
                        when (result.source) {
                            DashboardDataSource.CACHE -> {
                                "Loaded the mapped household snapshot from Apollo cache."
                            }

                            DashboardDataSource.NETWORK -> {
                                "Loaded household data from the server, wrote it into Apollo cache, and persisted the snapshot for cold starts."
                            }
                        },
                    isLoading = false,
                    error = null,
                )
            } catch (exception: IllegalStateException) {
                state.copy(
                    isLoading = false,
                    error = exception.message ?: "The GraphQL dashboard request failed.",
                )
            }
    }
}

@Composable
internal fun GraphqlDashboardCard(
    controller: GraphqlDashboardController,
    onLoadCacheFirst: () -> Unit,
    onReadCacheOnly: () -> Unit,
    onRefreshFromNetwork: () -> Unit,
) {
    val state = controller.state
    val loadedDashboardState = controller.loadedDashboardState

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.76f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "GraphQL household snapshot",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text =
                    "This now reaches past the family name: viewer, roster, balances, money accounts, tasks, " +
                        "and recent transaction activity are mapped into the dashboard state.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = controller.sessionStore.serverUrl,
                onValueChange = { controller.sessionStore.serverUrl = it },
                label = { Text("GraphQL URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = controller.sessionStore.jwt,
                onValueChange = { controller.sessionStore.jwt = it },
                label = { Text("JWT bearer token") },
                minLines = 2,
                maxLines = 3,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onLoadCacheFirst,
                    enabled = !state.isLoading,
                ) {
                    Text("Load")
                }
                OutlinedButton(
                    onClick = onReadCacheOnly,
                    enabled = !state.isLoading,
                ) {
                    Text("Read cache")
                }
                OutlinedButton(
                    onClick = onRefreshFromNetwork,
                    enabled = !state.isLoading,
                ) {
                    Text("Refresh")
                }
            }

            Text(
                text = state.status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.viewerSummary?.let { viewerSummary ->
                Text(
                    text = viewerSummary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.dashboardStateLoaded && loadedDashboardState != null) {
                Text(
                    text =
                        "Loaded ${loadedDashboardState.children.size} children, ${loadedDashboardState.accounts.size} accounts, " +
                            "${loadedDashboardState.tasks.size} tasks, and ${loadedDashboardState.activity.size} recent events.",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
