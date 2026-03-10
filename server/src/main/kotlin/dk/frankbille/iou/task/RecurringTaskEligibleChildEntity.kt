package dk.frankbille.iou.task

import dk.frankbille.iou.child.ChildEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@IdClass(RecurringTaskEligibleChildId::class)
@Table(name = "recurring_task_eligible_children")
class RecurringTaskEligibleChildEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_task_id", nullable = false)
    lateinit var recurringTask: RecurringTaskEntity

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    lateinit var child: ChildEntity
}
