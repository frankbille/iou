package dk.frankbille.iou.transaction

import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue("TRANSFER")
class TransferTransactionEntity : SingleAccountTransactionEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_two_id", nullable = false)
    lateinit var accountTwo: MoneyAccountEntity
}
