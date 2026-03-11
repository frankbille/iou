package dk.frankbille.iou.transaction

import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue("TRANSFER")
class TransferTransactionEntity : SingleAccountTransactionEntity() {
    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "account_two_id", nullable = false)
    lateinit var accountTwo: MoneyAccountEntity
}
