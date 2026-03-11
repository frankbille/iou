package dk.frankbille.iou.transaction

import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class SingleAccountTransactionEntity : TransactionEntity() {
    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "account_one_id", nullable = false)
    lateinit var accountOne: MoneyAccountEntity
}
