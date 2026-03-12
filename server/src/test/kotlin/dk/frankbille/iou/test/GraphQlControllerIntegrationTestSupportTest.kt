package dk.frankbille.iou.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GraphQlControllerIntegrationTestSupportTest : GraphQlControllerIntegrationTest() {
    @Test
    fun `tables to truncate includes join tables`() {
        assertThat(tablesToTruncate()).contains("tasks_eligible_children")
    }
}
