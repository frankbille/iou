package dk.frankbille.iou.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface RewardTransactionRepository : JpaRepository<RewardTransactionEntity, Long> {
    fun findByOneOffTaskId(oneOffTaskId: Long): RewardTransactionEntity?

    fun findByRecurringTaskCompletionId(recurringTaskCompletionId: Long): RewardTransactionEntity?
}
