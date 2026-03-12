package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.security.HasAccessToFamily
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
    private val moneyAccountRepository: MoneyAccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<MoneyAccount> =
        moneyAccountRepository.findAllByFamilyIdOrderByNameAsc(familyId).map { it.toDto() }

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
                    if (transaction.accountOne.id == accountId) transaction.amountMinor else 0
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
