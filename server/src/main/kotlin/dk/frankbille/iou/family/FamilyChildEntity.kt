package dk.frankbille.iou.family

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.task.RewardPayoutPolicy
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "family_children",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_family_children_family_child",
            columnNames = ["family_id", "child_id"],
        ),
    ],
)
class FamilyChildEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    lateinit var family: FamilyEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    lateinit var child: ChildEntity

    @Column(name = "relation", nullable = false)
    var relation: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_payout_policy_override", length = 32)
    var rewardPayoutPolicyOverride: RewardPayoutPolicy? = null
}
