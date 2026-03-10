package dk.frankbille.iou.transaction

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Entity
@DiscriminatorValue("ADJUSTMENT")
class AdjustmentTransactionEntity : SingleAccountTransactionEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_reason", length = 32, nullable = false)
    lateinit var adjustmentReason: AdjustmentReason
}
