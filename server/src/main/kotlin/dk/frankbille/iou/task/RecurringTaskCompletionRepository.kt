package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface RecurringTaskCompletionRepository : JpaRepository<RecurringTaskCompletionEntity, Long> {
    fun findAllByRecurringTaskIdOrderByOccurrenceDateDesc(recurringTaskId: Long): List<RecurringTaskCompletionEntity>

    fun existsByRecurringTaskId(recurringTaskId: Long): Boolean

    fun countByRecurringTaskIdAndOccurrenceDate(
        recurringTaskId: Long,
        occurrenceDate: LocalDate,
    ): Long

    fun findByRecurringTaskIdAndChildIdAndOccurrenceDate(
        recurringTaskId: Long,
        childId: Long,
        occurrenceDate: LocalDate,
    ): RecurringTaskCompletionEntity?

    @Query(
        """
        SELECT COUNT(rtc)
        FROM RecurringTaskCompletionEntity rtc
        JOIN RecurringTaskEntity rt ON rtc.recurringTaskId = rt.id
        WHERE rt.familyId = :familyId
          AND rtc.child.id = :childId
        """,
    )
    fun countByFamilyIdAndChildId(
        familyId: Long,
        childId: Long,
    ): Long

    @Query(
        """
        SELECT COUNT(rtc)
        FROM RecurringTaskCompletionEntity rtc
        JOIN RecurringTaskEntity rt ON rtc.recurringTaskId = rt.id
        WHERE rt.familyId = :familyId
          AND rtc.approvedByParent.id = :parentId
        """,
    )
    fun countByFamilyIdAndApprovedByParentId(
        familyId: Long,
        parentId: Long,
    ): Long

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
