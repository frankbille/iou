package dk.frankbille.iou.transaction

import dk.frankbille.iou.child.Child
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.parent.Parent
import kotlin.time.Instant

interface Transaction {
    val id: Long
    val familyId: Long
    val timestamp: Instant
    val owner: Parent
    val child: Child
    val amount: Money
    val description: String?
}

data class RewardTransaction(
    override val id: Long,
    override val familyId: Long,
    override val timestamp: Instant,
    override val owner: Parent,
    override val child: Child,
    override val amount: Money,
    override val description: String?,
    val toAccount: MoneyAccount,
    val oneOffTaskId: Long?,
    val recurringTaskCompletionId: Long?,
) : Transaction

data class TransferTransaction(
    override val id: Long,
    override val familyId: Long,
    override val timestamp: Instant,
    override val owner: Parent,
    override val child: Child,
    override val amount: Money,
    override val description: String?,
    val fromAccount: MoneyAccount,
    val toAccount: MoneyAccount,
) : Transaction

data class AdjustmentTransaction(
    override val id: Long,
    override val familyId: Long,
    override val timestamp: Instant,
    override val owner: Parent,
    override val child: Child,
    override val amount: Money,
    override val description: String?,
    val account: MoneyAccount,
    val reason: AdjustmentReason,
) : Transaction

data class WithdrawalTransaction(
    override val id: Long,
    override val familyId: Long,
    override val timestamp: Instant,
    override val owner: Parent,
    override val child: Child,
    override val amount: Money,
    override val description: String?,
    val fromAccount: MoneyAccount,
) : Transaction

data class DepositTransaction(
    override val id: Long,
    override val familyId: Long,
    override val timestamp: Instant,
    override val owner: Parent,
    override val child: Child,
    override val amount: Money,
    override val description: String?,
    val toAccount: MoneyAccount,
) : Transaction
