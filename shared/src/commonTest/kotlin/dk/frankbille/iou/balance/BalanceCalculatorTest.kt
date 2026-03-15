package dk.frankbille.iou.balance

import dk.frankbille.iou.child.Child
import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.moneyaccount.MoneyAccountKind
import dk.frankbille.iou.parent.Parent
import dk.frankbille.iou.transaction.AdjustmentReason
import dk.frankbille.iou.transaction.AdjustmentTransaction
import dk.frankbille.iou.transaction.DepositTransaction
import dk.frankbille.iou.transaction.RewardTransaction
import dk.frankbille.iou.transaction.TransferTransaction
import dk.frankbille.iou.transaction.WithdrawalTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BalanceCalculatorTest {
    private var nextId = 1L

    @Test
    fun `calculateChildBalance returns zero for no transactions`() {
        assertEquals(Money(0), BalanceCalculator.calculateChildBalance(emptyList()))
    }

    @Test
    fun `calculateChildBalance adds reward and deposit amounts`() {
        val transactions =
            listOf(
                reward(amountMinor = 500),
                deposit(amountMinor = 300),
            )

        assertEquals(Money(800), BalanceCalculator.calculateChildBalance(transactions))
    }

    @Test
    fun `calculateChildBalance subtracts withdrawals`() {
        val transactions =
            listOf(
                deposit(amountMinor = 500),
                withdrawal(amountMinor = 125),
            )

        assertEquals(Money(375), BalanceCalculator.calculateChildBalance(transactions))
    }

    @Test
    fun `calculateChildBalance ignores transfers`() {
        val transactions =
            listOf(
                deposit(amountMinor = 500),
                transfer(amountMinor = 200),
            )

        assertEquals(Money(500), BalanceCalculator.calculateChildBalance(transactions))
    }

    @Test
    fun `calculateChildBalance treats initial balance and manual add as credits`() {
        val transactions =
            listOf(
                adjustment(amountMinor = 200, reason = AdjustmentReason.INITIAL_BALANCE),
                adjustment(amountMinor = 75, reason = AdjustmentReason.MANUAL_ADD),
            )

        assertEquals(Money(275), BalanceCalculator.calculateChildBalance(transactions))
    }

    @Test
    fun `calculateChildBalance treats manual remove as a debit`() {
        val transactions =
            listOf(
                adjustment(amountMinor = 300, reason = AdjustmentReason.INITIAL_BALANCE),
                adjustment(amountMinor = 80, reason = AdjustmentReason.MANUAL_REMOVE),
            )

        assertEquals(Money(220), BalanceCalculator.calculateChildBalance(transactions))
    }

    @Test
    fun `calculateMoneyAccountBalance credits reward deposit and positive adjustments for destination account`() {
        val transactions =
            listOf(
                reward(amountMinor = 500, toAccount = PRIMARY_ACCOUNT),
                deposit(amountMinor = 300, toAccount = PRIMARY_ACCOUNT),
                adjustment(amountMinor = 125, reason = AdjustmentReason.MANUAL_ADD, account = PRIMARY_ACCOUNT),
                adjustment(amountMinor = 250, reason = AdjustmentReason.INITIAL_BALANCE, account = PRIMARY_ACCOUNT),
            )

        assertEquals(
            Money(1175),
            BalanceCalculator.calculateMoneyAccountBalance(accountId = PRIMARY_ACCOUNT.id, transactions = transactions),
        )
    }

    @Test
    fun `calculateMoneyAccountBalance debits withdrawals and manual removes for source account`() {
        val transactions =
            listOf(
                deposit(amountMinor = 500, toAccount = PRIMARY_ACCOUNT),
                withdrawal(amountMinor = 125, fromAccount = PRIMARY_ACCOUNT),
                adjustment(amountMinor = 80, reason = AdjustmentReason.MANUAL_REMOVE, account = PRIMARY_ACCOUNT),
            )

        assertEquals(
            Money(295),
            BalanceCalculator.calculateMoneyAccountBalance(accountId = PRIMARY_ACCOUNT.id, transactions = transactions),
        )
    }

    @Test
    fun `calculateMoneyAccountBalance moves transfer amount between accounts`() {
        val transactions = listOf(transfer(amountMinor = 200))

        assertEquals(
            Money(-200),
            BalanceCalculator.calculateMoneyAccountBalance(accountId = PRIMARY_ACCOUNT.id, transactions = transactions),
        )
        assertEquals(
            Money(200),
            BalanceCalculator.calculateMoneyAccountBalance(accountId = SAVINGS_ACCOUNT.id, transactions = transactions),
        )
    }

    @Test
    fun `calculateMoneyAccountBalance ignores transactions for other accounts`() {
        val transactions =
            listOf(
                reward(amountMinor = 500, toAccount = SAVINGS_ACCOUNT),
                deposit(amountMinor = 300, toAccount = SAVINGS_ACCOUNT),
                withdrawal(amountMinor = 75, fromAccount = SAVINGS_ACCOUNT),
                adjustment(amountMinor = 90, reason = AdjustmentReason.MANUAL_ADD, account = SAVINGS_ACCOUNT),
            )

        assertEquals(
            Money(0),
            BalanceCalculator.calculateMoneyAccountBalance(accountId = PRIMARY_ACCOUNT.id, transactions = transactions),
        )
    }

    private fun reward(
        amountMinor: Int,
        toAccount: MoneyAccount = PRIMARY_ACCOUNT,
    ): RewardTransaction =
        RewardTransaction(
            id = nextId++,
            familyId = FAMILY_ID,
            timestamp = TIMESTAMP,
            owner = PARENT,
            child = CHILD,
            amount = Money(amountMinor),
            description = null,
            toAccount = toAccount,
            oneOffTaskId = null,
            recurringTaskCompletionId = null,
        )

    private fun deposit(
        amountMinor: Int,
        toAccount: MoneyAccount = PRIMARY_ACCOUNT,
    ): DepositTransaction =
        DepositTransaction(
            id = nextId++,
            familyId = FAMILY_ID,
            timestamp = TIMESTAMP,
            owner = PARENT,
            child = CHILD,
            amount = Money(amountMinor),
            description = null,
            toAccount = toAccount,
        )

    private fun withdrawal(
        amountMinor: Int,
        fromAccount: MoneyAccount = PRIMARY_ACCOUNT,
    ): WithdrawalTransaction =
        WithdrawalTransaction(
            id = nextId++,
            familyId = FAMILY_ID,
            timestamp = TIMESTAMP,
            owner = PARENT,
            child = CHILD,
            amount = Money(amountMinor),
            description = null,
            fromAccount = fromAccount,
        )

    private fun transfer(
        amountMinor: Int,
        fromAccount: MoneyAccount = PRIMARY_ACCOUNT,
        toAccount: MoneyAccount = SAVINGS_ACCOUNT,
    ): TransferTransaction =
        TransferTransaction(
            id = nextId++,
            familyId = FAMILY_ID,
            timestamp = TIMESTAMP,
            owner = PARENT,
            child = CHILD,
            amount = Money(amountMinor),
            description = null,
            fromAccount = fromAccount,
            toAccount = toAccount,
        )

    private fun adjustment(
        amountMinor: Int,
        reason: AdjustmentReason,
        account: MoneyAccount = PRIMARY_ACCOUNT,
    ): AdjustmentTransaction =
        AdjustmentTransaction(
            id = nextId++,
            familyId = FAMILY_ID,
            timestamp = TIMESTAMP,
            owner = PARENT,
            child = CHILD,
            amount = Money(amountMinor),
            description = null,
            account = account,
            reason = reason,
        )

    companion object {
        private const val FAMILY_ID = 100L
        private val TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z")
        private val PARENT = Parent(id = 1, name = "Parent")
        private val CHILD = Child(id = 2, name = "Child")
        private val PRIMARY_ACCOUNT = MoneyAccount(id = 10, familyId = FAMILY_ID, name = "Primary", kind = MoneyAccountKind.CASH)
        private val SAVINGS_ACCOUNT =
            MoneyAccount(id = 11, familyId = FAMILY_ID, name = "Savings", kind = MoneyAccountKind.SAVINGS)
    }
}
