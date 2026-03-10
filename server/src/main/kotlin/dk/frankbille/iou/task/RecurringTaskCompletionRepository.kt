package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository

interface RecurringTaskCompletionRepository : JpaRepository<RecurringTaskCompletionEntity, Long> {
    fun findAllByRecurringTaskIdOrderByOccurrenceDateDesc(recurringTaskId: Long): List<RecurringTaskCompletionEntity>
}
