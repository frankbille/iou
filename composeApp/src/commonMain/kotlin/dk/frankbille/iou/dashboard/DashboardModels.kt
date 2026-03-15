package dk.frankbille.iou.dashboard

import androidx.compose.ui.graphics.Color

internal data class DashboardState(
    val familyName: String,
    val houseNote: String,
    val children: List<ChildSnapshot>,
    val tasks: List<TaskSnapshot>,
    val accounts: List<AccountSnapshot>,
    val activity: List<ActivitySnapshot>,
    val rules: List<RuleSnapshot>,
)

internal data class ChildSnapshot(
    val name: String,
    val balanceMinor: Int,
    val savedMinor: Int,
    val pendingTasks: Int,
    val streakDays: Int,
    val goalLabel: String,
    val accent: Color,
)

internal data class TaskSnapshot(
    val title: String,
    val childName: String,
    val timingLabel: String,
    val rewardMinor: Int,
    val status: TaskStatus,
    val accent: Color,
)

internal data class AccountSnapshot(
    val name: String,
    val amountMinor: Int,
    val note: String,
    val fillRatio: Float,
    val accent: Color,
)

internal data class ActivitySnapshot(
    val title: String,
    val detail: String,
    val amountLabel: String,
    val accent: Color,
)

internal data class RuleSnapshot(
    val title: String,
    val body: String,
    val accent: Color,
)

internal enum class TaskStatus(
    val label: String,
    val badgeColor: Color,
    val textColor: Color,
) {
    REQUIRES_APPROVAL("Needs approval", ClaySoft, Clay),
    SCHEDULED("On deck", PineSoft, Pine),
    AUTO_PAY("Auto pays", GoldSoft, Gold),
}

internal fun DashboardState.totalTrackedMinor(): Int = children.sumOf { it.balanceMinor }

internal fun DashboardState.pendingApprovals(): Int = tasks.count { it.status == TaskStatus.REQUIRES_APPROVAL }

internal fun DashboardState.totalScheduledRewardsMinor(): Int = tasks.sumOf { it.rewardMinor }

internal fun formatCurrency(
    amountMinor: Int,
    symbol: String = "$",
): String {
    val absoluteMinor = kotlin.math.abs(amountMinor)
    val whole = absoluteMinor / 100
    val cents = absoluteMinor % 100
    val prefix = if (amountMinor < 0) "-" else ""
    return "$prefix$symbol$whole.${cents.toString().padStart(2, '0')}"
}

internal fun ChildSnapshot.savedShare(): Float =
    if (balanceMinor <= 0) {
        0f
    } else {
        savedMinor.toFloat() / balanceMinor.toFloat()
    }
