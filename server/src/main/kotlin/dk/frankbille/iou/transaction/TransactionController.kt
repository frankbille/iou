package dk.frankbille.iou.transaction

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import dk.frankbille.iou.task.TaskCompletion
import dk.frankbille.iou.task.TaskService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class TransactionController(
    private val familyService: FamilyService,
    private val taskService: TaskService,
    private val transactionService: TransactionService,
) {
    @MutationMapping
    fun recordDeposit(
        @Argument @Valid input: RecordDepositInput,
    ): RecordDepositPayload = RecordDepositPayload(transactionService.recordDeposit(input))

    @MutationMapping
    fun recordWithdrawal(
        @Argument @Valid input: RecordWithdrawalInput,
    ): RecordWithdrawalPayload = RecordWithdrawalPayload(transactionService.recordWithdrawal(input))

    @MutationMapping
    fun recordTransfer(
        @Argument @Valid input: RecordTransferInput,
    ): RecordTransferPayload = RecordTransferPayload(transactionService.recordTransfer(input))

    @MutationMapping
    fun recordAdjustment(
        @Argument @Valid input: RecordAdjustmentInput,
    ): RecordAdjustmentPayload = RecordAdjustmentPayload(transactionService.recordAdjustment(input))

    @SchemaMapping(typeName = "RewardTransaction", field = "family")
    fun family(transaction: RewardTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "RewardTransaction", field = "taskCompletion")
    fun taskCompletion(transaction: RewardTransaction): TaskCompletion =
        when {
            transaction.oneOffTaskId != null -> taskService.getOneOffTask(transaction.oneOffTaskId)
            transaction.recurringTaskCompletionId != null -> taskService.getRecurringTaskCompletion(transaction.recurringTaskCompletionId)
            else -> throw IllegalStateException("Reward transaction ${transaction.id} is missing its task completion reference")
        }

    @SchemaMapping(typeName = "TransferTransaction", field = "family")
    fun family(transaction: TransferTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "AdjustmentTransaction", field = "family")
    fun family(transaction: AdjustmentTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "WithdrawalTransaction", field = "family")
    fun family(transaction: WithdrawalTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "DepositTransaction", field = "family")
    fun family(transaction: DepositTransaction): Family = familyService.getFamily(transaction.familyId)
}
