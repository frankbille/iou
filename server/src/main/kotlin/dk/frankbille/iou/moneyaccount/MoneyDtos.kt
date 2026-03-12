package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.family.CurrencyKind
import dk.frankbille.iou.family.CurrencyPosition
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val position: CurrencyPosition,
    val minorUnit: Int,
    val kind: CurrencyKind,
)

data class CurrencyInput(
    @field:NotBlank(message = "Currency code must not be blank")
    val code: String,
    @field:NotBlank(message = "Currency name must not be blank")
    val name: String,
    @field:NotBlank(message = "Currency symbol must not be blank")
    val symbol: String,
    val position: CurrencyPosition,
    @field:Min(value = 0, message = "Currency minor unit must be zero or greater")
    val minorUnit: Int,
    val kind: CurrencyKind,
)

data class Money(
    val amountMinor: Int,
)

data class MoneyAccount(
    val id: Long,
    val familyId: Long,
    val name: String,
    val kind: MoneyAccountKind,
)

@UniqueMoneyAccountName
data class CreateMoneyAccountInput(
    val familyId: Long,
    @field:NotBlank(message = "Money account name must not be blank")
    val name: String,
    val kind: MoneyAccountKind,
)

data class CreateMoneyAccountPayload(
    val moneyAccount: MoneyAccount,
)

@UniqueMoneyAccountName
data class UpdateMoneyAccountInput(
    val moneyAccountId: Long,
    @field:NotBlank(message = "Money account name must not be blank")
    val name: String,
    val kind: MoneyAccountKind,
)

data class UpdateMoneyAccountPayload(
    val moneyAccount: MoneyAccount,
)

data class DeleteMoneyAccountInput(
    val moneyAccountId: Long,
)

data class DeleteMoneyAccountPayload(
    val deletedMoneyAccountId: Long,
)
