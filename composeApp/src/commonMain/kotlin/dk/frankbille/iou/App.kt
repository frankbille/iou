@file:Suppress("FunctionName")

package dk.frankbille.iou

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import dk.frankbille.iou.dashboard.HouseholdDashboard
import dk.frankbille.iou.dashboard.IouTheme
import dk.frankbille.iou.dashboard.sampleDashboardState
import dk.frankbille.iou.graphql.FamilyNamePocCard
import dk.frankbille.iou.graphql.FamilyNamePocController
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val controller = remember { FamilyNamePocController() }
    val baseState = remember { sampleDashboardState() }
    var familyNameOverride by remember { mutableStateOf<String?>(null) }

    IouTheme {
        HouseholdDashboard(
            state = baseState.copy(familyName = familyNameOverride ?: baseState.familyName),
            topContent = {
                FamilyNamePocCard(
                    controller = controller,
                    onLoadCacheFirst = {
                        scope.launch {
                            controller.loadCacheFirst()
                            familyNameOverride = controller.state.familyName ?: familyNameOverride
                        }
                    },
                    onReadCacheOnly = {
                        scope.launch {
                            controller.readCacheOnly()
                            familyNameOverride = controller.state.familyName ?: familyNameOverride
                        }
                    },
                    onRefreshFromNetwork = {
                        scope.launch {
                            controller.refreshFromNetwork()
                            familyNameOverride = controller.state.familyName ?: familyNameOverride
                        }
                    },
                )
            },
        )
    }
}
