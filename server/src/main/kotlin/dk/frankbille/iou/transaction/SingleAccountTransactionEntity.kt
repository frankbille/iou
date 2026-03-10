package dk.frankbille.iou.transaction

import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class SingleAccountTransactionEntity : TransactionEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_one_id", nullable = false)
    lateinit var accountOne: MoneyAccountEntity
}
