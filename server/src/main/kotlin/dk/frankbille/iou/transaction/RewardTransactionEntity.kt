package dk.frankbille.iou.transaction

import dk.frankbille.iou.task.OneOffTaskEntity
import dk.frankbille.iou.task.RecurringTaskCompletionEntity
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity
@DiscriminatorValue("REWARD")
class RewardTransactionEntity : SingleAccountTransactionEntity() {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "one_off_task_id")
    var oneOffTask: OneOffTaskEntity? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_task_completion_id")
    var recurringTaskCompletion: RecurringTaskCompletionEntity? = null
}
