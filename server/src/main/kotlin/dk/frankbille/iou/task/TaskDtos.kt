package dk.frankbille.iou.task

import dk.frankbille.iou.child.Child
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.parent.Parent
import dk.frankbille.iou.taskcategory.TaskCategory
import dk.frankbille.iou.taskcategory.UniqueTaskCategoryName
import dk.frankbille.iou.transaction.RewardTransaction
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

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

@UniqueTaskCategoryName
data class UpdateTaskCategoryInput(
    val taskCategoryId: Long,
    @field:NotBlank(message = "Task category name must not be blank")
    val name: String,
)

data class UpdateTaskCategoryPayload(
    val taskCategory: TaskCategory,
)

data class DeleteTaskCategoryInput(
    val taskCategoryId: Long,
)

data class DeleteTaskCategoryPayload(
    val deletedTaskCategoryId: Long,
)

data class CreateOneOffTaskInput(
    val familyId: Long,
    @field:NotBlank(message = "Task title must not be blank")
    val title: String,
    val description: String?,
    val categoryId: Long,
    @field:Min(value = 1, message = "Task reward amount must be greater than zero")
    val rewardAmountMinor: Int,
    val rewardPayoutPolicy: RewardPayoutPolicy,
    val eligibleChildIds: List<Long>?,
)

data class CreateOneOffTaskPayload(
    val task: OneOffTask,
)

data class UpdateOneOffTaskInput(
    val taskId: Long,
    @field:NotBlank(message = "Task title must not be blank")
    val title: String,
    val description: String?,
    val categoryId: Long,
    @field:Min(value = 1, message = "Task reward amount must be greater than zero")
    val rewardAmountMinor: Int,
    val rewardPayoutPolicy: RewardPayoutPolicy,
    val eligibleChildIds: List<Long>?,
)

data class UpdateOneOffTaskPayload(
    val task: OneOffTask,
)

data class DeleteOneOffTaskInput(
    val taskId: Long,
)

data class DeleteOneOffTaskPayload(
    val deletedTaskId: Long,
)

data class CreateRecurringTaskInput(
    val familyId: Long,
    @field:NotBlank(message = "Task title must not be blank")
    val title: String,
    val description: String?,
    val categoryId: Long,
    @field:Min(value = 1, message = "Task reward amount must be greater than zero")
    val rewardAmountMinor: Int,
    val rewardPayoutPolicy: RewardPayoutPolicy,
    val eligibleChildIds: List<Long>?,
    @field:Valid
    val recurrence: TaskRecurrenceInput,
)

data class CreateRecurringTaskPayload(
    val task: RecurringTask,
)

data class UpdateRecurringTaskInput(
    val taskId: Long,
    @field:NotBlank(message = "Task title must not be blank")
    val title: String,
    val description: String?,
    val categoryId: Long,
    @field:Min(value = 1, message = "Task reward amount must be greater than zero")
    val rewardAmountMinor: Int,
    val rewardPayoutPolicy: RewardPayoutPolicy,
    val eligibleChildIds: List<Long>?,
    @field:Valid
    val recurrence: TaskRecurrenceInput,
)

data class UpdateRecurringTaskPayload(
    val task: RecurringTask,
)

data class ArchiveRecurringTaskInput(
    val taskId: Long,
)

data class ArchiveRecurringTaskPayload(
    val task: RecurringTask,
)

data class DeleteRecurringTaskInput(
    val taskId: Long,
)

data class DeleteRecurringTaskPayload(
    val deletedTaskId: Long,
)

@ValidTaskRecurrence
data class TaskRecurrenceInput(
    val kind: TaskRecurrenceKind,
    val interval: Int?,
    val daysOfWeek: List<DayOfWeek>?,
    val dayOfMonth: Int?,
    val startsOn: LocalDate?,
    val endsOn: LocalDate?,
    val maxCompletionsPerPeriod: Int?,
)

data class CompleteOneOffTaskInput(
    val taskId: Long,
    val childId: Long,
)

data class CompleteOneOffTaskPayload(
    val task: OneOffTask,
    val rewardTransaction: RewardTransaction?,
)

data class ApproveOneOffTaskInput(
    val taskId: Long,
)

data class ApproveOneOffTaskPayload(
    val task: OneOffTask,
    val rewardTransaction: RewardTransaction,
)

data class ResetOneOffTaskToAvailableInput(
    val taskId: Long,
)

data class ResetOneOffTaskToAvailablePayload(
    val task: OneOffTask,
)

data class CompleteRecurringTaskInput(
    val taskId: Long,
    val childId: Long,
    val occurrenceDate: LocalDate?,
)

data class CompleteRecurringTaskPayload(
    val completion: RecurringTaskCompletion,
    val rewardTransaction: RewardTransaction?,
)

data class ApproveRecurringTaskCompletionInput(
    val completionId: Long,
)

data class ApproveRecurringTaskCompletionPayload(
    val completion: RecurringTaskCompletion,
    val rewardTransaction: RewardTransaction,
)

data class ResetRecurringTaskCompletionToAvailableInput(
    val completionId: Long,
)

data class ResetRecurringTaskCompletionToAvailablePayload(
    val completion: RecurringTaskCompletion,
)
