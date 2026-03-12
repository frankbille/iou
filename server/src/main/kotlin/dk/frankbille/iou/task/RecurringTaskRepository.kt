package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RecurringTaskRepository : JpaRepository<RecurringTaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<RecurringTaskEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT t)
        FROM RecurringTaskEntity t
        LEFT JOIN t.eligibleChildren eligibleChild
        WHERE t.familyId = :familyId
          AND eligibleChild.id = :childId
        """,
    )
    fun countByFamilyIdAndEligibleChildId(
        familyId: Long,
        childId: Long,
    ): Long
}
