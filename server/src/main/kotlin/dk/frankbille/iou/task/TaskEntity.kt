package dk.frankbille.iou.task

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.task.EligibilityMode.ALL_CHILDREN
import dk.frankbille.iou.task.RewardPayoutPolicy.ON_COMPLETION
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType.STRING
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType.JOINED
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.Instant.EPOCH

@Entity
@Table(name = "tasks")
@Inheritance(strategy = JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = STRING, length = 32)
abstract class TaskEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "family_id", nullable = false)
    var familyId: Long = -1

    @Column(name = "title", nullable = false)
    var title: String = ""

    @Lob
    @Column(name = "description")
    var description: String? = null

    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "category_id")
    lateinit var category: TaskCategoryEntity

    @Column(name = "reward_amount_minor", nullable = false)
    var rewardAmountMinor: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_payout_policy", length = 32, nullable = false)
    var rewardPayoutPolicy: RewardPayoutPolicy = ON_COMPLETION

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_mode", length = 32, nullable = false)
    var eligibilityMode: EligibilityMode = ALL_CHILDREN

    @ManyToMany
    @JoinTable(
        name = "tasks_eligible_children",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "child_id")],
    )
    var eligibleChildren: MutableSet<ChildEntity>? = null

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by_parent_id", nullable = false)
    lateinit var createdByParent: ParentEntity

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = EPOCH

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "updated_by_parent_id", nullable = false)
    lateinit var updatedByParent: ParentEntity

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = EPOCH
}
