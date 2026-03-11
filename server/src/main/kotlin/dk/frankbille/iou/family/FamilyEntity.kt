package dk.frankbille.iou.family

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "families")
class FamilyEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Column(name = "currency_code", length = 32, nullable = false)
    var currencyCode: String = ""

    @Column(name = "currency_name", nullable = false)
    var currencyName: String = ""

    @Column(name = "currency_symbol", length = 32, nullable = false)
    var currencySymbol: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_position", length = 32, nullable = false)
    var currencyPosition: CurrencyPosition = PREFIX

    @Column(name = "currency_minor_unit", nullable = false)
    var currencyMinorUnit: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_kind", length = 32, nullable = false)
    var currencyKind: CurrencyKind = ISO_CURRENCY

    @OneToOne(fetch = EAGER)
    @JoinColumn(name = "default_reward_account_id")
    var defaultRewardAccount: MoneyAccountEntity? = null

    @Column(name = "recurring_task_completion_grace_period_days", nullable = false)
    var recurringTaskCompletionGracePeriodDays: Int = 0
}
