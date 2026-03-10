package dk.frankbille.iou.task

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "one_off_tasks")
class OneOffTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    lateinit var family: FamilyEntity

    @Column(name = "title", nullable = false)
    var title: String = ""

    @Lob
    @Column(name = "description")
    var description: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: TaskCategoryEntity? = null

    @Column(name = "reward_amount_minor", nullable = false)
    var rewardAmountMinor: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_payout_policy", length = 32, nullable = false)
    var rewardPayoutPolicy: RewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_mode", length = 32, nullable = false)
    var eligibilityMode: EligibilityMode = EligibilityMode.ALL_CHILDREN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_parent_id", nullable = false)
    lateinit var createdByParent: ParentEntity

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_parent_id", nullable = false)
    lateinit var updatedByParent: ParentEntity

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    var status: TaskCompletionStatus = TaskCompletionStatus.AVAILABLE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_child_id")
    var completedChild: ChildEntity? = null

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    @Column(name = "approved_at")
    var approvedAt: Instant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_parent_id")
    var approvedByParent: ParentEntity? = null
}
