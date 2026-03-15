package dk.frankbille.iou.transaction

import jakarta.validation.constraints.Min

data class RecordDepositInput(
    val familyId: Long,
    val childId: Long,
    val toAccountId: Long,
    @field:Min(value = 1, message = "Transaction amount must be greater than zero")
    val amountMinor: Int,
    val description: String?,
)

data class RecordDepositPayload(
    val transaction: DepositTransaction,
)

data class RecordWithdrawalInput(
    val familyId: Long,
    val childId: Long,
    val fromAccountId: Long,
    @field:Min(value = 1, message = "Transaction amount must be greater than zero")
    val amountMinor: Int,
    val description: String?,
)

data class RecordWithdrawalPayload(
    val transaction: WithdrawalTransaction,
)

data class RecordTransferInput(
    val familyId: Long,
    val childId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    @field:Min(value = 1, message = "Transaction amount must be greater than zero")
    val amountMinor: Int,
    val description: String?,
)

data class RecordTransferPayload(
    val transaction: TransferTransaction,
)

data class RecordAdjustmentInput(
    val familyId: Long,
    val childId: Long,
    val accountId: Long,
    @field:Min(value = 1, message = "Transaction amount must be greater than zero")
    val amountMinor: Int,
    val reason: AdjustmentReason,
    val description: String?,
)

data class RecordAdjustmentPayload(
    val transaction: AdjustmentTransaction,
)
