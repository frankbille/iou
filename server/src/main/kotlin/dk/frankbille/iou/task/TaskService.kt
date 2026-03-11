package dk.frankbille.iou.task

import dk.frankbille.iou.child.toDto
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.taskcategory.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaskService(
    private val taskRepository: TaskRepository,
    private val recurringTaskCompletionRepository: RecurringTaskCompletionRepository,
) {
    fun getByFamilyId(familyId: Long): List<Task> = taskRepository.findAllByFamilyIdOrderByCreatedAtDesc(familyId).map { it.toDto() }

    fun getCompletions(taskId: Long): List<RecurringTaskCompletion> =
        recurringTaskCompletionRepository.findAllByRecurringTaskIdOrderByOccurrenceDateDesc(taskId).map { it.toDto() }
}

fun TaskEntity.toDto(): Task =
    when (this) {
        is OneOffTaskEntity -> toDto()
        is RecurringTaskEntity -> toDto()
        else -> error("Unsupported task type: ${this::class.simpleName}")
    }

fun OneOffTaskEntity.toDto(): OneOffTask =
    OneOffTask(
        id = requireNotNull(id),
        title = title,
        description = description,
        reward = Money(rewardAmountMinor),
        rewardPayoutPolicy = rewardPayoutPolicy,
        createdAt = createdAt,
        createdBy = createdByParent.toDto(),
        updatedAt = updatedAt,
        updatedBy = updatedByParent.toDto(),
        status = status,
        completedAt = completedAt,
        child = completedChild?.toDto(),
        approvedAt = approvedAt,
        approvedBy = approvedByParent?.toDto(),
        familyId = familyId,
        category = category.toDto(),
    )

fun RecurringTaskEntity.toDto(): RecurringTask =
    RecurringTask(
        id = requireNotNull(id),
        title = title,
        description = description,
        reward = Money(rewardAmountMinor),
        rewardPayoutPolicy = rewardPayoutPolicy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = status,
        recurrence =
            TaskRecurrence(
                kind = recurrenceKind,
                interval = recurrenceInterval,
                daysOfWeek = null,
                dayOfMonth = recurrenceDayOfMonth,
                startsOn = recurrenceStartsOn,
                endsOn = recurrenceEndsOn,
                maxCompletionsPerPeriod = recurrenceMaxCompletionsPerPeriod,
            ),
        familyId = familyId,
        category = category.toDto(),
        createdByParentId = requireNotNull(createdByParent.id),
        updatedByParentId = requireNotNull(updatedByParent.id),
    )

fun RecurringTaskCompletionEntity.toDto(): RecurringTaskCompletion =
    RecurringTaskCompletion(
        id = requireNotNull(id),
        recurringTaskId = recurringTaskId,
        child = child.toDto(),
        occurrenceDate = occurrenceDate,
        status = status,
        completedAt = completedAt,
        approvedAt = approvedAt,
        approvedBy = approvedByParent?.toDto(),
    )
