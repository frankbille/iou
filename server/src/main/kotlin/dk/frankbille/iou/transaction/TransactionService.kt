package dk.frankbille.iou.transaction

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.toDto
import dk.frankbille.iou.events.FamilyEventRecorder
import dk.frankbille.iou.events.TransactionRecordedEvent
import dk.frankbille.iou.family.FamilyChildRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import dk.frankbille.iou.moneyaccount.toDto
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.CurrentViewer
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import dk.frankbille.iou.task.OneOffTaskEntity
import dk.frankbille.iou.task.RecurringTaskCompletionEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val familyRepository: FamilyRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val parentRepository: ParentRepository,
    private val transactionRepository: TransactionRepository,
    private val rewardTransactionRepository: RewardTransactionRepository,
    private val currentViewer: CurrentViewer,
    private val familyEventRecorder: FamilyEventRecorder,
) {
    fun getByChildId(childId: Long): List<Transaction> =
        transactionRepository.findAllByChildIdOrderByTimestampDesc(childId).map { it.toDto() }

    fun getRewardByOneOffTaskId(oneOffTaskId: Long): RewardTransaction? =
        rewardTransactionRepository.findByOneOffTaskId(oneOffTaskId)?.toDto()

    fun getRewardByRecurringTaskCompletionId(completionId: Long): RewardTransaction? =
        rewardTransactionRepository.findByRecurringTaskCompletionId(completionId)?.toDto()

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

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun recordDeposit(input: RecordDepositInput): DepositTransaction =
        transactionRepository
            .save(
                DepositTransactionEntity().apply {
                    familyId = input.familyId
                    ownerParent = currentParentEntity()
                    child = resolveFamilyChild(input.familyId, input.childId)
                    accountOne = resolveMoneyAccount(input.toAccountId, input.familyId)
                    amountMinor = input.amountMinor
                    description = input.description?.trim()?.takeIf { it.isNotEmpty() }
                    timestamp = Instant.now()
                },
            ).toDto()
            .also {
                familyEventRecorder.record(TransactionRecordedEvent(it))
            }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun recordWithdrawal(input: RecordWithdrawalInput): WithdrawalTransaction =
        transactionRepository
            .save(
                WithdrawalTransactionEntity().apply {
                    familyId = input.familyId
                    ownerParent = currentParentEntity()
                    child = resolveFamilyChild(input.familyId, input.childId)
                    accountOne = resolveMoneyAccount(input.fromAccountId, input.familyId)
                    amountMinor = input.amountMinor
                    description = input.description?.trim()?.takeIf { it.isNotEmpty() }
                    timestamp = Instant.now()
                },
            ).toDto()
            .also {
                familyEventRecorder.record(TransactionRecordedEvent(it))
            }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun recordTransfer(input: RecordTransferInput): TransferTransaction {
        if (input.fromAccountId == input.toAccountId) {
            throw IllegalArgumentException("Transfer source and destination accounts must differ")
        }

        return transactionRepository
            .save(
                TransferTransactionEntity().apply {
                    familyId = input.familyId
                    ownerParent = currentParentEntity()
                    child = resolveFamilyChild(input.familyId, input.childId)
                    accountOne = resolveMoneyAccount(input.fromAccountId, input.familyId)
                    accountTwo = resolveMoneyAccount(input.toAccountId, input.familyId)
                    amountMinor = input.amountMinor
                    description = input.description?.trim()?.takeIf { it.isNotEmpty() }
                    timestamp = Instant.now()
                },
            ).toDto()
            .also {
                familyEventRecorder.record(TransactionRecordedEvent(it))
            }
    }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun recordAdjustment(input: RecordAdjustmentInput): AdjustmentTransaction =
        transactionRepository
            .save(
                AdjustmentTransactionEntity().apply {
                    familyId = input.familyId
                    ownerParent = currentParentEntity()
                    child = resolveFamilyChild(input.familyId, input.childId)
                    accountOne = resolveMoneyAccount(input.accountId, input.familyId)
                    amountMinor = input.amountMinor
                    adjustmentReason = input.reason
                    description = input.description?.trim()?.takeIf { it.isNotEmpty() }
                    timestamp = Instant.now()
                },
            ).toDto()
            .also {
                familyEventRecorder.record(TransactionRecordedEvent(it))
            }

    @Transactional
    fun createRewardTransaction(
        familyId: Long,
        child: ChildEntity,
        ownerParent: ParentEntity,
        amountMinor: Int,
        oneOffTask: OneOffTaskEntity? = null,
        recurringTaskCompletion: RecurringTaskCompletionEntity? = null,
    ): RewardTransaction {
        val family = familyRepository.findById(familyId).orElseThrow()
        val defaultRewardAccount =
            family.defaultRewardAccount
                ?: throw IllegalArgumentException("Family $familyId does not have a default reward account configured")

        return rewardTransactionRepository
            .save(
                RewardTransactionEntity().apply {
                    this.familyId = familyId
                    this.ownerParent = ownerParent
                    this.child = child
                    accountOne = defaultRewardAccount
                    this.amountMinor = amountMinor
                    timestamp = Instant.now()
                    this.oneOffTask = oneOffTask
                    this.recurringTaskCompletion = recurringTaskCompletion
                },
            ).toDto()
            .also {
                familyEventRecorder.record(TransactionRecordedEvent(it))
            }
    }

    private fun currentParentEntity(): ParentEntity =
        parentRepository
            .findById(currentViewer.parentId())
            .orElseThrow { AccessDeniedException("Authenticated parent ${currentViewer.parentId()} was not found") }

    private fun resolveFamilyChild(
        familyId: Long,
        childId: Long,
    ): ChildEntity =
        familyChildRepository
            .findByFamilyIdAndChildId(familyId, childId)
            ?.child
            ?: throw IllegalArgumentException("Child $childId does not belong to family $familyId")

    private fun resolveMoneyAccount(
        moneyAccountId: Long,
        familyId: Long,
    ): MoneyAccountEntity {
        val moneyAccount = moneyAccountRepository.findById(moneyAccountId).orElseThrow()
        if (moneyAccount.familyId != familyId) {
            throw IllegalArgumentException("Money account $moneyAccountId does not belong to family $familyId")
        }

        return moneyAccount
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
