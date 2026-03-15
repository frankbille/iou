package dk.frankbille.iou.task

import dk.frankbille.iou.child.Child
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.parent.Parent
import dk.frankbille.iou.taskcategory.TaskCategory
import dk.frankbille.iou.transaction.RewardTransaction
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface Task {
    val id: Long
    val familyId: Long
    val title: String
    val description: String?
    val category: TaskCategory
    val reward: Money
    val rewardPayoutPolicy: RewardPayoutPolicy
    val createdBy: Parent
    val createdAt: Instant
    val updatedBy: Parent
    val updatedAt: Instant
    val eligibleChildren: List<Child>?
}

interface TaskCompletion {
    val child: Child?
    val status: TaskCompletionStatus
    val completedAt: Instant?
    val approvedAt: Instant?
    val approvedBy: Parent?
    val rewardTransaction: RewardTransaction?
}

data class OneOffTask(
    override val id: Long,
    override val familyId: Long,
    override val title: String,
    override val description: String?,
    override val category: TaskCategory,
    override val reward: Money,
    override val rewardPayoutPolicy: RewardPayoutPolicy,
    override val createdBy: Parent,
    override val createdAt: Instant,
    override val updatedBy: Parent,
    override val updatedAt: Instant,
    override val eligibleChildren: List<Child>? = null,
    override val child: Child?,
    override val status: TaskCompletionStatus,
    override val completedAt: Instant?,
    override val approvedAt: Instant?,
    override val approvedBy: Parent?,
    override val rewardTransaction: RewardTransaction? = null,
) : Task,
    TaskCompletion

data class RecurringTask(
    override val id: Long,
    override val familyId: Long,
    override val title: String,
    override val description: String?,
    override val category: TaskCategory,
    override val reward: Money,
    override val rewardPayoutPolicy: RewardPayoutPolicy,
    override val createdBy: Parent,
    override val createdAt: Instant,
    override val updatedBy: Parent,
    override val updatedAt: Instant,
    override val eligibleChildren: List<Child>? = null,
    val recurringTaskStatus: RecurringTaskStatus,
    val recurrence: TaskRecurrence,
) : Task

data class TaskRecurrence(
    val kind: TaskRecurrenceKind,
    val interval: Int?,
    val daysOfWeek: List<DayOfWeek>?,
    val dayOfMonth: Int?,
    val startsOn: LocalDate?,
    val endsOn: LocalDate?,
    val maxCompletionsPerPeriod: Int?,
)

data class RecurringTaskCompletion(
    val id: Long,
    val recurringTaskId: Long,
    override val child: Child,
    val occurrenceDate: LocalDate,
    override val status: TaskCompletionStatus,
    override val completedAt: Instant?,
    override val approvedAt: Instant?,
    override val approvedBy: Parent? = null,
    override val rewardTransaction: RewardTransaction? = null,
) : TaskCompletion
