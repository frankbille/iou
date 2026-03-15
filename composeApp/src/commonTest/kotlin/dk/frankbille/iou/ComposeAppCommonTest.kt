package dk.frankbille.iou.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {
    @Test
    fun sampleDashboardState_exposesExpectedSummary() {
        val state = sampleDashboardState()

        assertEquals("Birch House", state.familyName)
        assertEquals(13_715, state.totalTrackedMinor())
        assertEquals(2, state.pendingApprovals())
        assertEquals(875, state.totalScheduledRewardsMinor())
    }

    @Test
    fun formatCurrency_formatsMinorUnits() {
        assertEquals("$137.15", formatCurrency(13_715))
        assertEquals("-$2.50", formatCurrency(-250))
    }
}
