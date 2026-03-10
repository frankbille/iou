package dk.frankbille.iou.family

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "families")
class FamilyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    var currencyPosition: CurrencyPosition = CurrencyPosition.PREFIX

    @Column(name = "currency_minor_unit", nullable = false)
    var currencyMinorUnit: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_kind", length = 32, nullable = false)
    var currencyKind: CurrencyKind = CurrencyKind.ISO_CURRENCY

    // Stored as a scalar to keep the family/account mapping free of circular persistence dependencies.
    @Column(name = "default_reward_account_id")
    var defaultRewardAccountId: Long? = null

    @Column(name = "recurring_task_completion_grace_period_days", nullable = false)
    var recurringTaskCompletionGracePeriodDays: Int = 0
}
