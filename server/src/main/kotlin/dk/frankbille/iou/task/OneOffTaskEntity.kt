package dk.frankbille.iou.task

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.task.TaskCompletionStatus.AVAILABLE
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import java.time.Instant

@Entity
@DiscriminatorValue("ONE_OFF")
@Table(name = "one_off_tasks")
@PrimaryKeyJoinColumn(name = "task_id")
class OneOffTaskEntity : TaskEntity() {
    @Enumerated(STRING)
    @Column(name = "status", length = 32, nullable = false)
    var status: TaskCompletionStatus = AVAILABLE

    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "completed_child_id")
    var completedChild: ChildEntity? = null

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    @Column(name = "approved_at")
    var approvedAt: Instant? = null

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "approved_by_parent_id")
    var approvedByParent: ParentEntity? = null
}
