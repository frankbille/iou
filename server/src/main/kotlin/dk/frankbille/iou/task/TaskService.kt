package dk.frankbille.iou.task

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.ChildRepository
import dk.frankbille.iou.child.toDto
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.family.FamilyChildRepository
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.CurrentViewer
import dk.frankbille.iou.security.FamilyScopeCheck
import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import dk.frankbille.iou.taskcategory.TaskCategoryRepository
import dk.frankbille.iou.taskcategory.toDto
import dk.frankbille.iou.transaction.RewardTransactionRepository
import dk.frankbille.iou.transaction.TransactionService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class TaskService(
    private val taskRepository: TaskRepository,
    private val oneOffTaskRepository: OneOffTaskRepository,
    private val recurringTaskRepository: RecurringTaskRepository,
    private val recurringTaskCompletionRepository: RecurringTaskCompletionRepository,
    private val rewardTransactionRepository: RewardTransactionRepository,
    private val familyRepository: FamilyRepository,
    private val taskCategoryRepository: TaskCategoryRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val childRepository: ChildRepository,
    private val parentRepository: ParentRepository,
    private val currentViewer: CurrentViewer,
    private val transactionService: TransactionService,
) {
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<Task> = taskRepository.findAllByFamilyIdOrderByCreatedAtDesc(familyId).map { it.toDto() }

    fun getCompletions(taskId: Long): List<RecurringTaskCompletion> =
        recurringTaskCompletionRepository.findAllByRecurringTaskIdOrderByOccurrenceDateDesc(taskId).map { it.toDto() }

    fun getOneOffTask(taskId: Long): OneOffTask = oneOffTaskRepository.findById(taskId).orElseThrow().toDto()

    fun getRecurringTask(taskId: Long): RecurringTask = recurringTaskRepository.findById(taskId).orElseThrow().toDto()

    fun getRecurringTaskCompletion(completionId: Long): RecurringTaskCompletion =
        recurringTaskCompletionRepository.findById(completionId).orElseThrow().toDto()

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun createOneOffTask(input: CreateOneOffTaskInput): OneOffTask {
        val parent = currentParentEntity()
        val taskConfig = resolveTaskDefinitionConfig(input.familyId, input.categoryId, input.eligibleChildIds)
        val now = Instant.now()

        return oneOffTaskRepository
            .save(
                OneOffTaskEntity().apply {
                    familyId = input.familyId
                    title = input.title.trim()
                    description = input.description?.trim()?.takeIf { it.isNotEmpty() }
                    category = taskConfig.category
                    rewardAmountMinor = input.rewardAmountMinor
                    rewardPayoutPolicy = input.rewardPayoutPolicy
                    eligibilityMode = taskConfig.eligibilityMode
                    eligibleChildren = taskConfig.eligibleChildren
                    createdByParent = parent
                    createdAt = now
                    updatedByParent = parent
                    updatedAt = now
                },
            ).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun updateOneOffTask(input: UpdateOneOffTaskInput): OneOffTask {
        val task = oneOffTaskRepository.findById(input.taskId).orElseThrow()
        val parent = currentParentEntity()
        val taskConfig = resolveTaskDefinitionConfig(task.familyId, input.categoryId, input.eligibleChildIds)

        task.apply {
            title = input.title.trim()
            description = input.description?.trim()?.takeIf { it.isNotEmpty() }
            category = taskConfig.category
            rewardAmountMinor = input.rewardAmountMinor
            rewardPayoutPolicy = input.rewardPayoutPolicy
            eligibilityMode = taskConfig.eligibilityMode
            eligibleChildren = taskConfig.eligibleChildren
            updatedByParent = parent
            updatedAt = Instant.now()
        }

        return oneOffTaskRepository.save(task).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun deleteOneOffTask(input: DeleteOneOffTaskInput): Long {
        if (rewardTransactionRepository.findByOneOffTaskId(input.taskId) != null) {
            throw IllegalArgumentException("Cannot delete task ${input.taskId} because it already has a reward transaction")
        }

        val task = oneOffTaskRepository.findById(input.taskId).orElseThrow()
        oneOffTaskRepository.delete(task)
        return requireNotNull(task.id)
    }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun createRecurringTask(input: CreateRecurringTaskInput): RecurringTask {
        val parent = currentParentEntity()
        val taskConfig = resolveTaskDefinitionConfig(input.familyId, input.categoryId, input.eligibleChildIds)
        val now = Instant.now()

        return recurringTaskRepository
            .save(
                RecurringTaskEntity().apply {
                    familyId = input.familyId
                    title = input.title.trim()
                    description = input.description?.trim()?.takeIf { it.isNotEmpty() }
                    category = taskConfig.category
                    rewardAmountMinor = input.rewardAmountMinor
                    rewardPayoutPolicy = input.rewardPayoutPolicy
                    eligibilityMode = taskConfig.eligibilityMode
                    eligibleChildren = taskConfig.eligibleChildren
                    createdByParent = parent
                    createdAt = now
                    updatedByParent = parent
                    updatedAt = now
                    status = RecurringTaskStatus.ACTIVE
                    applyRecurrence(input.recurrence)
                },
            ).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun updateRecurringTask(input: UpdateRecurringTaskInput): RecurringTask {
        val task = recurringTaskRepository.findById(input.taskId).orElseThrow()
        val parent = currentParentEntity()
        val taskConfig = resolveTaskDefinitionConfig(task.familyId, input.categoryId, input.eligibleChildIds)
        val completionExists = recurringTaskCompletionRepository.existsByRecurringTaskId(task.id ?: -1)

        if (completionExists && recurrenceChanged(task, input.recurrence)) {
            throw IllegalArgumentException("Cannot change recurrence for task ${input.taskId} after completions exist")
        }

        task.apply {
            title = input.title.trim()
            description = input.description?.trim()?.takeIf { it.isNotEmpty() }
            category = taskConfig.category
            rewardAmountMinor = input.rewardAmountMinor
            rewardPayoutPolicy = input.rewardPayoutPolicy
            eligibilityMode = taskConfig.eligibilityMode
            eligibleChildren = taskConfig.eligibleChildren
            if (!completionExists) {
                applyRecurrence(input.recurrence)
            }
            updatedByParent = parent
            updatedAt = Instant.now()
        }

        return recurringTaskRepository.save(task).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun archiveRecurringTask(input: ArchiveRecurringTaskInput): RecurringTask {
        val task = recurringTaskRepository.findById(input.taskId).orElseThrow()
        task.status = RecurringTaskStatus.ARCHIVED
        task.updatedByParent = currentParentEntity()
        task.updatedAt = Instant.now()
        return recurringTaskRepository.save(task).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun deleteRecurringTask(input: DeleteRecurringTaskInput): Long {
        if (recurringTaskCompletionRepository.existsByRecurringTaskId(input.taskId)) {
            throw IllegalArgumentException("Cannot delete task ${input.taskId} because it already has completions")
        }

        val task = recurringTaskRepository.findById(input.taskId).orElseThrow()
        recurringTaskRepository.delete(task)
        return requireNotNull(task.id)
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun completeOneOffTask(input: CompleteOneOffTaskInput): CompleteOneOffTaskPayload {
        val task = oneOffTaskRepository.findById(input.taskId).orElseThrow()
        if (task.status != TaskCompletionStatus.AVAILABLE) {
            throw IllegalArgumentException("One-off task ${input.taskId} is not available for completion")
        }

        val childMembership = resolveFamilyChildMembership(task.familyId, input.childId)
        ensureChildEligible(task, input.childId)
        val completedAt = Instant.now()
        task.completedChild = childMembership.child
        task.completedAt = completedAt
        task.status = TaskCompletionStatus.COMPLETED
        task.updatedByParent = currentParentEntity()
        task.updatedAt = completedAt

        val rewardTransaction =
            when (effectiveRewardPayoutPolicy(task.familyId, input.childId, task.rewardPayoutPolicy)) {
                RewardPayoutPolicy.ON_COMPLETION ->
                    transactionService.createRewardTransaction(
                        familyId = task.familyId,
                        child = childMembership.child,
                        amountMinor = task.rewardAmountMinor,
                        oneOffTask = task,
                    )

                RewardPayoutPolicy.ON_APPROVAL -> null
            }

        val savedTask = oneOffTaskRepository.save(task).toDto()
        return CompleteOneOffTaskPayload(savedTask, rewardTransaction)
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun approveOneOffTask(input: ApproveOneOffTaskInput): ApproveOneOffTaskPayload {
        val task = oneOffTaskRepository.findById(input.taskId).orElseThrow()
        if (task.status != TaskCompletionStatus.COMPLETED) {
            throw IllegalArgumentException("One-off task ${input.taskId} is not awaiting approval")
        }

        val completedChild = task.completedChild ?: throw IllegalArgumentException("One-off task ${input.taskId} has no completed child")
        if (effectiveRewardPayoutPolicy(task.familyId, requireNotNull(completedChild.id), task.rewardPayoutPolicy) != RewardPayoutPolicy.ON_APPROVAL) {
            throw IllegalArgumentException("One-off task ${input.taskId} does not require approval for reward payout")
        }

        val approvalTime = Instant.now()
        task.status = TaskCompletionStatus.APPROVED
        task.approvedAt = approvalTime
        task.approvedByParent = currentParentEntity()
        task.updatedByParent = requireNotNull(task.approvedByParent)
        task.updatedAt = approvalTime

        val rewardTransaction =
            transactionService.createRewardTransaction(
                familyId = task.familyId,
                child = completedChild,
                amountMinor = task.rewardAmountMinor,
                oneOffTask = task,
            )

        val savedTask = oneOffTaskRepository.save(task).toDto()
        return ApproveOneOffTaskPayload(savedTask, rewardTransaction)
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun resetOneOffTaskToAvailable(input: ResetOneOffTaskToAvailableInput): OneOffTask {
        if (rewardTransactionRepository.findByOneOffTaskId(input.taskId) != null) {
            throw IllegalArgumentException("One-off task ${input.taskId} cannot be reset because it already has a reward transaction")
        }

        val task = oneOffTaskRepository.findById(input.taskId).orElseThrow()
        task.status = TaskCompletionStatus.AVAILABLE
        task.completedChild = null
        task.completedAt = null
        task.approvedAt = null
        task.approvedByParent = null
        task.updatedByParent = currentParentEntity()
        task.updatedAt = Instant.now()
        return oneOffTaskRepository.save(task).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskFamilyId(#input.taskId)")
    fun completeRecurringTask(input: CompleteRecurringTaskInput): CompleteRecurringTaskPayload {
        val task = recurringTaskRepository.findById(input.taskId).orElseThrow()
        if (task.status != RecurringTaskStatus.ACTIVE) {
            throw IllegalArgumentException("Recurring task ${input.taskId} is not active")
        }

        val childMembership = resolveFamilyChildMembership(task.familyId, input.childId)
        ensureChildEligible(task, input.childId)
        val family = familyRepository.findById(task.familyId).orElseThrow()
        val occurrenceDate = resolveOccurrenceDate(task, family.recurringTaskCompletionGracePeriodDays, input.occurrenceDate)

        if (recurringTaskCompletionRepository.findByRecurringTaskIdAndChildIdAndOccurrenceDate(requireNotNull(task.id), input.childId, occurrenceDate) != null) {
            throw IllegalArgumentException(
                "Recurring task ${input.taskId} already has a completion for child ${input.childId} on $occurrenceDate",
            )
        }

        task.recurrenceMaxCompletionsPerPeriod?.let { max ->
            if (recurringTaskCompletionRepository.countByRecurringTaskIdAndOccurrenceDate(requireNotNull(task.id), occurrenceDate) >= max) {
                throw IllegalArgumentException("Recurring task ${input.taskId} has reached its completion limit for $occurrenceDate")
            }
        }

        val completion =
            recurringTaskCompletionRepository.save(
                RecurringTaskCompletionEntity().apply {
                    recurringTaskId = requireNotNull(task.id)
                    child = childMembership.child
                    this.occurrenceDate = occurrenceDate
                    status = TaskCompletionStatus.COMPLETED
                    completedAt = Instant.now()
                },
            )

        val rewardTransaction =
            when (effectiveRewardPayoutPolicy(task.familyId, input.childId, task.rewardPayoutPolicy)) {
                RewardPayoutPolicy.ON_COMPLETION ->
                    transactionService.createRewardTransaction(
                        familyId = task.familyId,
                        child = childMembership.child,
                        amountMinor = task.rewardAmountMinor,
                        recurringTaskCompletion = completion,
                    )

                RewardPayoutPolicy.ON_APPROVAL -> null
            }

        return CompleteRecurringTaskPayload(completion.toDto(), rewardTransaction)
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.recurringTaskCompletionFamilyId(#input.completionId)")
    fun approveRecurringTaskCompletion(input: ApproveRecurringTaskCompletionInput): ApproveRecurringTaskCompletionPayload {
        val completion = recurringTaskCompletionRepository.findById(input.completionId).orElseThrow()
        if (completion.status != TaskCompletionStatus.COMPLETED) {
            throw IllegalArgumentException("Recurring task completion ${input.completionId} is not awaiting approval")
        }

        val task = recurringTaskRepository.findById(completion.recurringTaskId).orElseThrow()
        if (effectiveRewardPayoutPolicy(task.familyId, requireNotNull(completion.child.id), task.rewardPayoutPolicy) != RewardPayoutPolicy.ON_APPROVAL) {
            throw IllegalArgumentException("Recurring task completion ${input.completionId} does not require approval for reward payout")
        }

        val approvalTime = Instant.now()
        completion.status = TaskCompletionStatus.APPROVED
        completion.approvedAt = approvalTime
        completion.approvedByParent = currentParentEntity()

        val rewardTransaction =
            transactionService.createRewardTransaction(
                familyId = task.familyId,
                child = completion.child,
                amountMinor = task.rewardAmountMinor,
                recurringTaskCompletion = completion,
            )

        val savedCompletion = recurringTaskCompletionRepository.save(completion).toDto()
        return ApproveRecurringTaskCompletionPayload(savedCompletion, rewardTransaction)
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.recurringTaskCompletionFamilyId(#input.completionId)")
    fun resetRecurringTaskCompletionToAvailable(input: ResetRecurringTaskCompletionToAvailableInput): RecurringTaskCompletion {
        if (rewardTransactionRepository.findByRecurringTaskCompletionId(input.completionId) != null) {
            throw IllegalArgumentException(
                "Recurring task completion ${input.completionId} cannot be reset because it already has a reward transaction",
            )
        }

        val completion = recurringTaskCompletionRepository.findById(input.completionId).orElseThrow()
        completion.status = TaskCompletionStatus.AVAILABLE
        completion.completedAt = null
        completion.approvedAt = null
        completion.approvedByParent = null
        return recurringTaskCompletionRepository.save(completion).toDto()
    }

    private fun resolveTaskDefinitionConfig(
        familyId: Long,
        categoryId: Long,
        eligibleChildIds: List<Long>?,
    ): TaskDefinitionConfig {
        val category = resolveCategory(categoryId)
        if (category.familyId != familyId) {
            throw IllegalArgumentException("Task category $categoryId does not belong to family $familyId")
        }

        return when (eligibleChildIds) {
            null -> TaskDefinitionConfig(category, EligibilityMode.ALL_CHILDREN, null)
            else -> {
                val uniqueChildIds = eligibleChildIds.toSet()
                val familyChildIds = familyChildRepository.findChildIdsByFamilyIdAndChildIds(familyId, uniqueChildIds).toSet()
                val missingChildIds = uniqueChildIds - familyChildIds
                if (missingChildIds.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Eligible children ${missingChildIds.sorted()} do not belong to family $familyId",
                    )
                }

                val childrenById = childRepository.findAllById(uniqueChildIds).associateBy { requireNotNull(it.id) }
                val eligibleChildren =
                    eligibleChildIds
                        .distinct()
                        .mapNotNull(childrenById::get)
                        .toMutableSet()
                TaskDefinitionConfig(category, EligibilityMode.RESTRICTED, eligibleChildren)
            }
        }
    }

    private fun resolveCategory(categoryId: Long): TaskCategoryEntity = taskCategoryRepository.findById(categoryId).orElseThrow()

    private fun effectiveRewardPayoutPolicy(
        familyId: Long,
        childId: Long,
        taskPolicy: RewardPayoutPolicy,
    ): RewardPayoutPolicy = familyChildRepository.findByFamilyIdAndChildId(familyId, childId)?.rewardPayoutPolicyOverride ?: taskPolicy

    private fun resolveFamilyChildMembership(
        familyId: Long,
        childId: Long,
    ) = familyChildRepository.findByFamilyIdAndChildId(familyId, childId)
        ?: throw IllegalArgumentException("Child $childId does not belong to family $familyId")

    private fun ensureChildEligible(
        task: TaskEntity,
        childId: Long,
    ) {
        if (task.eligibilityMode == EligibilityMode.ALL_CHILDREN) {
            return
        }

        if (task.eligibleChildren.orEmpty().none { it.id == childId }) {
            throw IllegalArgumentException("Child $childId is not eligible to complete task ${requireNotNull(task.id)}")
        }
    }

    private fun resolveOccurrenceDate(
        task: RecurringTaskEntity,
        gracePeriodDays: Int,
        requestedOccurrenceDate: LocalDate?,
    ): LocalDate {
        val today = LocalDate.now()
        val earliestAllowedDate = today.minusDays(gracePeriodDays.toLong())

        requestedOccurrenceDate?.let { occurrenceDate ->
            validateOccurrenceDate(task, occurrenceDate, today, earliestAllowedDate)
            return occurrenceDate
        }

        return generateSequence(today) { current ->
            current.minusDays(1)
        }.takeWhile { candidate ->
            !candidate.isBefore(earliestAllowedDate)
        }.firstOrNull { candidate ->
            isWithinActiveRange(task, candidate) && matchesRecurrence(task, candidate)
        }
            ?: throw IllegalArgumentException(
                "No eligible occurrence date is available for recurring task ${requireNotNull(task.id)} within the family's grace period",
            )
    }

    private fun validateOccurrenceDate(
        task: RecurringTaskEntity,
        occurrenceDate: LocalDate,
        today: LocalDate,
        earliestAllowedDate: LocalDate,
    ) {
        if (occurrenceDate.isAfter(today)) {
            throw IllegalArgumentException("Occurrence date cannot be in the future")
        }

        if (occurrenceDate.isBefore(earliestAllowedDate)) {
            throw IllegalArgumentException("Occurrence date is outside the family's recurring task grace period")
        }

        if (!isWithinActiveRange(task, occurrenceDate)) {
            task.recurrenceStartsOn?.let { startsOn ->
                if (occurrenceDate.isBefore(startsOn)) {
                    throw IllegalArgumentException("Occurrence date $occurrenceDate is before the task recurrence start date")
                }
            }
            task.recurrenceEndsOn?.let { endsOn ->
                if (occurrenceDate.isAfter(endsOn)) {
                    throw IllegalArgumentException("Occurrence date $occurrenceDate is after the task recurrence end date")
                }
            }
        }

        if (!matchesRecurrence(task, occurrenceDate)) {
            throw IllegalArgumentException("Occurrence date $occurrenceDate does not match the task recurrence")
        }
    }

    private fun isWithinActiveRange(
        task: RecurringTaskEntity,
        occurrenceDate: LocalDate,
    ): Boolean =
        !occurrenceDate.isBefore(task.recurrenceStartsOn ?: LocalDate.MIN) &&
            !occurrenceDate.isAfter(task.recurrenceEndsOn ?: LocalDate.MAX)

    private fun matchesRecurrence(
        task: RecurringTaskEntity,
        occurrenceDate: LocalDate,
    ): Boolean =
        when (task.recurrenceKind) {
            TaskRecurrenceKind.DAILY ->
                matchesInterval(
                    startDate = task.recurrenceStartsOn,
                    occurrenceDate = occurrenceDate,
                    interval = task.recurrenceInterval,
                    unit = ChronoUnit.DAYS,
                )

            TaskRecurrenceKind.WEEKLY ->
                occurrenceDate.dayOfWeek in (task.recurrenceDays ?: emptySet()) &&
                    matchesInterval(
                        startDate = task.recurrenceStartsOn,
                        occurrenceDate = occurrenceDate,
                        interval = task.recurrenceInterval,
                        unit = ChronoUnit.WEEKS,
                    )

            TaskRecurrenceKind.MONTHLY ->
                occurrenceDate.dayOfMonth == task.recurrenceDayOfMonth &&
                    matchesInterval(
                        startDate = task.recurrenceStartsOn?.withDayOfMonth(1),
                        occurrenceDate = occurrenceDate.withDayOfMonth(1),
                        interval = task.recurrenceInterval,
                        unit = ChronoUnit.MONTHS,
                    )

            TaskRecurrenceKind.CUSTOM -> {
                val byWeekday =
                    task.recurrenceDays?.takeIf { it.isNotEmpty() }?.let {
                        occurrenceDate.dayOfWeek in it &&
                            matchesInterval(task.recurrenceStartsOn, occurrenceDate, task.recurrenceInterval, ChronoUnit.WEEKS)
                    }
                val byMonthDay =
                    task.recurrenceDayOfMonth?.let {
                        occurrenceDate.dayOfMonth == it &&
                            matchesInterval(task.recurrenceStartsOn?.withDayOfMonth(1), occurrenceDate.withDayOfMonth(1), task.recurrenceInterval, ChronoUnit.MONTHS)
                    }

                byWeekday ?: byMonthDay ?: matchesInterval(task.recurrenceStartsOn, occurrenceDate, task.recurrenceInterval, ChronoUnit.DAYS)
            }
        }

    private fun matchesInterval(
        startDate: LocalDate?,
        occurrenceDate: LocalDate,
        interval: Int?,
        unit: ChronoUnit,
    ): Boolean {
        if (interval == null || interval == 1 || startDate == null) {
            return true
        }

        if (occurrenceDate.isBefore(startDate)) {
            return false
        }

        return unit.between(startDate, occurrenceDate) % interval.toLong() == 0L
    }

    private fun currentParentEntity(): ParentEntity =
        parentRepository
            .findById(currentViewer.parentId())
            .orElseThrow { AccessDeniedException("Authenticated parent ${currentViewer.parentId()} was not found") }

    private fun RecurringTaskEntity.applyRecurrence(recurrence: TaskRecurrenceInput) {
        recurrenceKind = recurrence.kind
        recurrenceInterval = recurrence.interval
        recurrenceDayOfMonth = recurrence.dayOfMonth
        recurrenceDays = recurrence.daysOfWeek?.toSet()
        recurrenceStartsOn = recurrence.startsOn
        recurrenceEndsOn = recurrence.endsOn
        recurrenceMaxCompletionsPerPeriod = recurrence.maxCompletionsPerPeriod
    }

    private fun recurrenceChanged(
        task: RecurringTaskEntity,
        recurrence: TaskRecurrenceInput,
    ): Boolean =
        task.recurrenceKind != recurrence.kind ||
            task.recurrenceInterval != recurrence.interval ||
            task.recurrenceDayOfMonth != recurrence.dayOfMonth ||
            (task.recurrenceDays ?: emptySet<java.time.DayOfWeek>()) !=
            (recurrence.daysOfWeek?.toSet() ?: emptySet<java.time.DayOfWeek>()) ||
            task.recurrenceStartsOn != recurrence.startsOn ||
            task.recurrenceEndsOn != recurrence.endsOn ||
            task.recurrenceMaxCompletionsPerPeriod != recurrence.maxCompletionsPerPeriod

    private data class TaskDefinitionConfig(
        val category: TaskCategoryEntity,
        val eligibilityMode: EligibilityMode,
        val eligibleChildren: MutableSet<ChildEntity>?,
    )
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
        eligibleChildren = eligibleChildren.toDto(eligibilityMode),
    )

fun RecurringTaskEntity.toDto(): RecurringTask =
    RecurringTask(
        id = requireNotNull(id),
        title = title,
        description = description,
        reward = Money(rewardAmountMinor),
        rewardPayoutPolicy = rewardPayoutPolicy,
        createdAt = createdAt,
        createdBy = createdByParent.toDto(),
        updatedAt = updatedAt,
        updatedBy = updatedByParent.toDto(),
        eligibleChildren = eligibleChildren.toDto(eligibilityMode),
        recurringTaskStatus = status,
        recurrence =
            TaskRecurrence(
                kind = recurrenceKind,
                interval = recurrenceInterval,
                daysOfWeek = recurrenceDays?.takeIf { it.isNotEmpty() }?.sortedBy { it.value },
                dayOfMonth = recurrenceDayOfMonth,
                startsOn = recurrenceStartsOn,
                endsOn = recurrenceEndsOn,
                maxCompletionsPerPeriod = recurrenceMaxCompletionsPerPeriod,
            ),
        familyId = familyId,
        category = category.toDto(),
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

private fun MutableSet<ChildEntity>?.toDto(eligibilityMode: EligibilityMode): List<dk.frankbille.iou.child.Child>? =
    when (eligibilityMode) {
        EligibilityMode.ALL_CHILDREN -> null
        EligibilityMode.RESTRICTED -> this?.map { it.toDto() }?.sortedBy { it.id } ?: emptyList()
    }
