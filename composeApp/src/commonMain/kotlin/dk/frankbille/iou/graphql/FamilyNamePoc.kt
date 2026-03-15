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

internal data class FamilyNamePocState(
    val familyName: String? = null,
    val status: String = "JWT + normalized cache POC. On web, a network load is also persisted for cold-start cache reads.",
    val isLoading: Boolean = false,
    val error: String? = null,
)

internal class FamilyNamePocController(
    val sessionStore: GraphqlSessionStore = GraphqlSessionStore(),
) {
    private val repository = FamilyNameRepository(sessionStore = sessionStore)

    var state by mutableStateOf(FamilyNamePocState())
        private set

    suspend fun loadCacheFirst() {
        runRequest { repository.loadFamilyName() }
    }

    suspend fun readCacheOnly() {
        runRequest { repository.readCachedFamilyName() }
    }

    suspend fun refreshFromNetwork() {
        runRequest { repository.refreshFamilyName() }
    }

    private suspend fun runRequest(block: suspend () -> FamilyNameResult) {
        state = state.copy(isLoading = true, error = null)

        state =
            try {
                val result = block()
                FamilyNamePocState(
                    familyName = result.familyName,
                    status =
                        when (result.source) {
                            FamilyNameSource.CACHE -> {
                                "Loaded the family name from Apollo cache."
                            }

                            FamilyNameSource.NETWORK -> {
                                "Loaded the family name from the server, wrote it into Apollo cache, and persisted it for web cold starts."
                            }
                        },
                    isLoading = false,
                    error = null,
                )
            } catch (exception: IllegalStateException) {
                state.copy(
                    isLoading = false,
                    error = exception.message ?: "The family name request failed.",
                )
            }
    }
}

@Composable
internal fun FamilyNamePocCard(
    controller: FamilyNamePocController,
    onLoadCacheFirst: () -> Unit,
    onReadCacheOnly: () -> Unit,
    onRefreshFromNetwork: () -> Unit,
) {
    val state = controller.state

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
                text = "GraphQL family-name POC",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text =
                    "This is intentionally narrow: authenticated Apollo query, normalized cache, " +
                        "and a family name that survives browser reloads by hydrating Apollo on startup.",
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

            state.familyName?.let { familyName ->
                Text(
                    text = "Loaded family: $familyName",
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
