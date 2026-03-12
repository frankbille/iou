package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OneOffTaskRepository : JpaRepository<OneOffTaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<OneOffTaskEntity>

    @Query(
        """
        SELECT COUNT(t)
        FROM OneOffTaskEntity t
        WHERE t.familyId = :familyId
          AND t.approvedByParent.id = :parentId
        """,
    )
    fun countByFamilyIdAndApprovedByParentId(
        familyId: Long,
        parentId: Long,
    ): Long

    @Query(
        """
        SELECT COUNT(DISTINCT t)
        FROM OneOffTaskEntity t
        LEFT JOIN t.eligibleChildren eligibleChild
        WHERE t.familyId = :familyId
          AND (t.completedChild.id = :childId OR eligibleChild.id = :childId)
        """,
    )
    fun countByFamilyIdAndChildReferences(
        familyId: Long,
        childId: Long,
    ): Long
}
