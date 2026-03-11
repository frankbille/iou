package dk.frankbille.iou.transaction

import dk.frankbille.iou.child.toDto
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.moneyaccount.toDto
import dk.frankbille.iou.parent.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val transactionRepository: TransactionRepository,
) {
    fun getByChildId(childId: Long): List<Transaction> =
        transactionRepository.findAllByChildIdOrderByTimestampDesc(childId).map { it.toDto() }

    fun calculateChildBalance(childId: Long): Int =
        getByChildId(childId).sumOf {
            when (it) {
                is RewardTransaction -> {
                    it.amount.amountMinor
                }

                is DepositTransaction -> {
                    it.amount.amountMinor
                }

                is AdjustmentTransaction -> {
                    when (it.reason) {
                        AdjustmentReason.MANUAL_REMOVE -> -it.amount.amountMinor
                        else -> it.amount.amountMinor
                    }
                }

                is WithdrawalTransaction -> {
                    -it.amount.amountMinor
                }

                is TransferTransaction -> {
                    0
                }

                else -> {
                    0
                }
            }
        }
}

fun TransactionEntity.toDto(): Transaction =
    when (this) {
        is RewardTransactionEntity -> toDto()
        is TransferTransactionEntity -> toDto()
        is AdjustmentTransactionEntity -> toDto()
        is WithdrawalTransactionEntity -> toDto()
        is DepositTransactionEntity -> toDto()
        else -> error("Unsupported transaction type: ${this::class.simpleName}")
    }

fun RewardTransactionEntity.toDto(): RewardTransaction =
    RewardTransaction(
        id = requireNotNull(id),
        timestamp = timestamp,
        amount = Money(amountMinor),
        description = description,
        familyId = familyId,
        owner = ownerParent.toDto(),
        child = child.toDto(),
        toAccount = accountOne.toDto(),
        oneOffTaskId = oneOffTask?.id,
        recurringTaskCompletionId = recurringTaskCompletion?.id,
    )

fun TransferTransactionEntity.toDto(): TransferTransaction =
    TransferTransaction(
        id = requireNotNull(id),
        timestamp = timestamp,
        amount = Money(amountMinor),
        description = description,
        familyId = familyId,
        owner = ownerParent.toDto(),
        child = child.toDto(),
        fromAccount = accountOne.toDto(),
        toAccount = accountTwo.toDto(),
    )

fun AdjustmentTransactionEntity.toDto(): AdjustmentTransaction =
    AdjustmentTransaction(
        id = requireNotNull(id),
        timestamp = timestamp,
        amount = Money(amountMinor),
        description = description,
        reason = adjustmentReason,
        familyId = familyId,
        owner = ownerParent.toDto(),
        child = child.toDto(),
        account = accountOne.toDto(),
    )

fun WithdrawalTransactionEntity.toDto(): WithdrawalTransaction =
    WithdrawalTransaction(
        id = requireNotNull(id),
        timestamp = timestamp,
        amount = Money(amountMinor),
        description = description,
        familyId = familyId,
        owner = ownerParent.toDto(),
        child = child.toDto(),
        fromAccount = accountOne.toDto(),
    )

fun DepositTransactionEntity.toDto(): DepositTransaction =
    DepositTransaction(
        id = requireNotNull(id),
        timestamp = timestamp,
        amount = Money(amountMinor),
        description = description,
        familyId = familyId,
        owner = ownerParent.toDto(),
        child = child.toDto(),
        toAccount = accountOne.toDto(),
    )
