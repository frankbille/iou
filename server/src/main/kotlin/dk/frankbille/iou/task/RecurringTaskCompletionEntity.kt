package dk.frankbille.iou.task

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.parent.ParentEntity
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
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "recurring_task_completions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_recurring_task_completions_task_child_occurrence",
            columnNames = ["recurring_task_id", "child_id", "occurrence_date"],
        ),
    ],
)
class RecurringTaskCompletionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_task_id", nullable = false)
    lateinit var recurringTask: RecurringTaskEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    lateinit var child: ChildEntity

    @Column(name = "occurrence_date", nullable = false)
    var occurrenceDate: LocalDate = LocalDate.of(1970, 1, 1)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    var status: TaskCompletionStatus = TaskCompletionStatus.AVAILABLE

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    @Column(name = "approved_at")
    var approvedAt: Instant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_parent_id")
    var approvedByParent: ParentEntity? = null
}
