package dk.frankbille.iou.balance

import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.transaction.AdjustmentTransaction
import dk.frankbille.iou.transaction.DepositTransaction
import dk.frankbille.iou.transaction.RewardTransaction
import dk.frankbille.iou.transaction.Transaction
import dk.frankbille.iou.transaction.TransferTransaction
import dk.frankbille.iou.transaction.WithdrawalTransaction

object BalanceCalculator {
    fun calculateChildBalance(transactions: Iterable<Transaction>): Money = Money(transactions.sumOf(::childBalanceDelta))

    fun calculateMoneyAccountBalance(
        accountId: Long,
        transactions: Iterable<Transaction>,
    ): Money = Money(transactions.sumOf { accountBalanceDelta(accountId, it) })

    private fun childBalanceDelta(transaction: Transaction): Int =
        when (transaction) {
            is RewardTransaction -> {
                transaction.amount.amountMinor
            }

            is DepositTransaction -> {
                transaction.amount.amountMinor
            }

            is AdjustmentTransaction -> {
                if (transaction.reason == dk.frankbille.iou.transaction.AdjustmentReason.MANUAL_REMOVE) {
                    -transaction.amount.amountMinor
                } else {
                    transaction.amount.amountMinor
                }
            }

            is WithdrawalTransaction -> {
                -transaction.amount.amountMinor
            }

            is TransferTransaction -> {
                0
            }

            else -> {
                error("Unsupported transaction type: ${transaction::class.simpleName}")
            }
        }

    private fun accountBalanceDelta(
        accountId: Long,
        transaction: Transaction,
    ): Int =
        when (transaction) {
            is RewardTransaction -> {
                if (transaction.toAccount.id == accountId) {
                    transaction.amount.amountMinor
                } else {
                    0
                }
            }

            is DepositTransaction -> {
                if (transaction.toAccount.id == accountId) {
                    transaction.amount.amountMinor
                } else {
                    0
                }
            }

            is AdjustmentTransaction -> {
                if (transaction.account.id != accountId) {
                    0
                } else if (transaction.reason == dk.frankbille.iou.transaction.AdjustmentReason.MANUAL_REMOVE) {
                    -transaction.amount.amountMinor
                } else {
                    transaction.amount.amountMinor
                }
            }

            is WithdrawalTransaction -> {
                if (transaction.fromAccount.id == accountId) {
                    -transaction.amount.amountMinor
                } else {
                    0
                }
            }

            is TransferTransaction -> {
                when (accountId) {
                    transaction.fromAccount.id -> -transaction.amount.amountMinor
                    transaction.toAccount.id -> transaction.amount.amountMinor
                    else -> 0
                }
            }

            else -> {
                error("Unsupported transaction type: ${transaction::class.simpleName}")
            }
        }
}
