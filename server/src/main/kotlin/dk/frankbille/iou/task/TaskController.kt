package dk.frankbille.iou.task

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import dk.frankbille.iou.transaction.RewardTransaction
import dk.frankbille.iou.transaction.TransactionService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class TaskController(
    private val familyService: FamilyService,
    private val taskService: TaskService,
    private val transactionService: TransactionService,
) {
    @MutationMapping
    fun createOneOffTask(
        @Argument @Valid input: CreateOneOffTaskInput,
    ): CreateOneOffTaskPayload = CreateOneOffTaskPayload(taskService.createOneOffTask(input))

    @MutationMapping
    fun updateOneOffTask(
        @Argument @Valid input: UpdateOneOffTaskInput,
    ): UpdateOneOffTaskPayload = UpdateOneOffTaskPayload(taskService.updateOneOffTask(input))

    @MutationMapping
    fun deleteOneOffTask(
        @Argument input: DeleteOneOffTaskInput,
    ): DeleteOneOffTaskPayload = DeleteOneOffTaskPayload(taskService.deleteOneOffTask(input))

    @MutationMapping
    fun createRecurringTask(
        @Argument @Valid input: CreateRecurringTaskInput,
    ): CreateRecurringTaskPayload = CreateRecurringTaskPayload(taskService.createRecurringTask(input))

    @MutationMapping
    fun updateRecurringTask(
        @Argument @Valid input: UpdateRecurringTaskInput,
    ): UpdateRecurringTaskPayload = UpdateRecurringTaskPayload(taskService.updateRecurringTask(input))

    @MutationMapping
    fun archiveRecurringTask(
        @Argument input: ArchiveRecurringTaskInput,
    ): ArchiveRecurringTaskPayload = ArchiveRecurringTaskPayload(taskService.archiveRecurringTask(input))

    @MutationMapping
    fun deleteRecurringTask(
        @Argument input: DeleteRecurringTaskInput,
    ): DeleteRecurringTaskPayload = DeleteRecurringTaskPayload(taskService.deleteRecurringTask(input))

    @MutationMapping
    fun completeOneOffTask(
        @Argument @Valid input: CompleteOneOffTaskInput,
    ): CompleteOneOffTaskPayload = taskService.completeOneOffTask(input)

    @MutationMapping
    fun approveOneOffTask(
        @Argument @Valid input: ApproveOneOffTaskInput,
    ): ApproveOneOffTaskPayload = taskService.approveOneOffTask(input)

    @MutationMapping
    fun resetOneOffTaskToAvailable(
        @Argument @Valid input: ResetOneOffTaskToAvailableInput,
    ): ResetOneOffTaskToAvailablePayload = ResetOneOffTaskToAvailablePayload(taskService.resetOneOffTaskToAvailable(input))

    @MutationMapping
    fun completeRecurringTask(
        @Argument @Valid input: CompleteRecurringTaskInput,
    ): CompleteRecurringTaskPayload = taskService.completeRecurringTask(input)

    @MutationMapping
    fun approveRecurringTaskCompletion(
        @Argument @Valid input: ApproveRecurringTaskCompletionInput,
    ): ApproveRecurringTaskCompletionPayload = taskService.approveRecurringTaskCompletion(input)

    @MutationMapping
    fun resetRecurringTaskCompletionToAvailable(
        @Argument @Valid input: ResetRecurringTaskCompletionToAvailableInput,
    ): ResetRecurringTaskCompletionToAvailablePayload =
        ResetRecurringTaskCompletionToAvailablePayload(taskService.resetRecurringTaskCompletionToAvailable(input))

    @SchemaMapping(typeName = "OneOffTask", field = "family")
    fun family(task: OneOffTask): Family = familyService.getFamily(task.familyId)

    @SchemaMapping(typeName = "RecurringTask", field = "family")
    fun family(task: RecurringTask): Family = familyService.getFamily(task.familyId)

    @SchemaMapping(typeName = "RecurringTask", field = "completions")
    fun completions(task: RecurringTask): List<RecurringTaskCompletion> = taskService.getCompletions(task.id)

    @SchemaMapping(typeName = "OneOffTask", field = "rewardTransaction")
    fun rewardTransaction(task: OneOffTask): RewardTransaction? = transactionService.getRewardByOneOffTaskId(task.id)

    @SchemaMapping(typeName = "RecurringTaskCompletion", field = "rewardTransaction")
    fun rewardTransaction(completion: RecurringTaskCompletion): RewardTransaction? =
        transactionService.getRewardByRecurringTaskCompletionId(completion.id)

    @SchemaMapping(typeName = "RecurringTaskCompletion", field = "task")
    fun task(completion: RecurringTaskCompletion): RecurringTask = taskService.getRecurringTask(completion.recurringTaskId)
}
