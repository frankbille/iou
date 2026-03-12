package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RecurringTaskCompletionRepository : JpaRepository<RecurringTaskCompletionEntity, Long> {
    fun findAllByRecurringTaskIdOrderByOccurrenceDateDesc(recurringTaskId: Long): List<RecurringTaskCompletionEntity>

    @Query(
        """
        SELECT rt.familyId
        FROM RecurringTaskCompletionEntity rtc
        JOIN RecurringTaskEntity rt ON rtc.recurringTaskId = rt.id
        WHERE rtc.id = :id
        """,
    )
    fun findFamilyIdById(id: Long): Long?
}
