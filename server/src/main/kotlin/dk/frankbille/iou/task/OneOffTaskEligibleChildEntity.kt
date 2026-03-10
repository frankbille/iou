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
@IdClass(OneOffTaskEligibleChildId::class)
@Table(name = "one_off_task_eligible_children")
class OneOffTaskEligibleChildEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "one_off_task_id", nullable = false)
    lateinit var oneOffTask: OneOffTaskEntity

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    lateinit var child: ChildEntity
}
