package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.security.FamilyScopeCheck
import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import dk.frankbille.iou.transaction.AdjustmentReason
import dk.frankbille.iou.transaction.AdjustmentTransactionEntity
import dk.frankbille.iou.transaction.DepositTransactionEntity
import dk.frankbille.iou.transaction.RewardTransactionEntity
import dk.frankbille.iou.transaction.TransactionRepository
import dk.frankbille.iou.transaction.TransferTransactionEntity
import dk.frankbille.iou.transaction.WithdrawalTransactionEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MoneyAccountService(
    private val familyRepository: FamilyRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<MoneyAccount> =
        moneyAccountRepository.findAllByFamilyIdOrderByNameAsc(familyId).map { it.toDto() }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun createMoneyAccount(input: CreateMoneyAccountInput): MoneyAccount {
        familyRepository.findById(input.familyId).orElseThrow()

        return moneyAccountRepository
            .save(
                MoneyAccountEntity().apply {
                    familyId = input.familyId
                    name = input.name.trim()
                    kind = input.kind
                },
            ).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.moneyAccountFamilyId(#input.moneyAccountId)")
    fun updateMoneyAccount(input: UpdateMoneyAccountInput): MoneyAccount {
        val moneyAccount = moneyAccountRepository.findById(input.moneyAccountId).orElseThrow()
        moneyAccount.name = input.name.trim()
        moneyAccount.kind = input.kind
        return moneyAccountRepository.save(moneyAccount).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.moneyAccountFamilyId(#input.moneyAccountId)")
    fun deleteMoneyAccount(input: DeleteMoneyAccountInput): Long {
        val moneyAccount = moneyAccountRepository.findById(input.moneyAccountId).orElseThrow()
        val family = familyRepository.findById(moneyAccount.familyId).orElseThrow()
        val moneyAccountId = requireNotNull(moneyAccount.id)

        if (family.defaultRewardAccount?.id == moneyAccountId) {
            throw IllegalArgumentException(
                "Cannot delete money account $moneyAccountId because it is the default reward account for family ${family.id}",
            )
        }

        if (transactionRepository.countReferencesByMoneyAccountId(moneyAccountId) > 0) {
            throw IllegalArgumentException(
                "Cannot delete money account $moneyAccountId because it is referenced by transactions",
            )
        }

        moneyAccountRepository.delete(moneyAccount)
        return moneyAccountId
    }

    @HasAccessToFamily
    fun getBalance(
        accountId: Long,
        familyId: Long,
    ) = Money(
        transactionRepository.findAllByFamilyIdOrderByTimestampDesc(familyId).sumOf { transaction ->
            when (transaction) {
                is RewardTransactionEntity -> {
                    if (transaction.accountOne.id == accountId) transaction.amountMinor else 0
                }

                is DepositTransactionEntity -> {
                    if (transaction.accountOne.id == accountId) transaction.amountMinor else 0
                }

                is AdjustmentTransactionEntity -> {
                    if (transaction.accountOne.id == accountId) {
                        when (transaction.adjustmentReason) {
                            AdjustmentReason.MANUAL_REMOVE -> -transaction.amountMinor
                            else -> transaction.amountMinor
                        }
                    } else {
                        0
                    }
                }

                is TransferTransactionEntity -> {
                    when (accountId) {
                        transaction.accountOne.id -> -transaction.amountMinor
                        transaction.accountTwo.id -> transaction.amountMinor
                        else -> 0
                    }
                }

                is WithdrawalTransactionEntity -> {
                    if (transaction.accountOne.id == accountId) -transaction.amountMinor else 0
                }

                else -> {
                    0
                }
            }
        },
    )
}

fun MoneyAccountEntity.toDto(): MoneyAccount =
    MoneyAccount(
        id = requireNotNull(id),
        name = name,
        kind = kind,
        familyId = familyId,
    )
