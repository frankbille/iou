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
import dk.frankbille.iou.graphql.GraphqlDashboardCard
import dk.frankbille.iou.graphql.GraphqlDashboardController
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val controller = remember { GraphqlDashboardController() }
    val baseState = remember { sampleDashboardState() }
    var dashboardOverride by remember { mutableStateOf<dk.frankbille.iou.dashboard.DashboardState?>(null) }

    IouTheme {
        HouseholdDashboard(
            state = dashboardOverride ?: baseState,
            topContent = {
                GraphqlDashboardCard(
                    controller = controller,
                    onLoadCacheFirst = {
                        scope.launch {
                            controller.loadCacheFirst()
                            dashboardOverride = controller.loadedDashboardState ?: dashboardOverride
                        }
                    },
                    onReadCacheOnly = {
                        scope.launch {
                            controller.readCacheOnly()
                            dashboardOverride = controller.loadedDashboardState ?: dashboardOverride
                        }
                    },
                    onRefreshFromNetwork = {
                        scope.launch {
                            controller.refreshFromNetwork()
                            dashboardOverride = controller.loadedDashboardState ?: dashboardOverride
                        }
                    },
                )
            },
        )
    }
}
