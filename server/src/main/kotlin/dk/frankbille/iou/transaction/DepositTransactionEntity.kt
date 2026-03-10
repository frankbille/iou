package dk.frankbille.iou.transaction

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("DEPOSIT")
class DepositTransactionEntity : SingleAccountTransactionEntity()
