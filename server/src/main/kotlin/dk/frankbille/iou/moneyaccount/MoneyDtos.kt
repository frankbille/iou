package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.family.CurrencyKind
import dk.frankbille.iou.family.CurrencyPosition

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val position: CurrencyPosition,
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
