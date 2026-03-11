package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.moneyaccount.MoneyAccountKind.CASH
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "money_accounts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_money_accounts_family_name",
            columnNames = ["family_id", "name"],
        ),
    ],
)
class MoneyAccountEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "family_id", nullable = false)
    var familyId: Long = -1

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Enumerated(STRING)
    @Column(name = "kind", length = 32, nullable = false)
    var kind: MoneyAccountKind = CASH
}
