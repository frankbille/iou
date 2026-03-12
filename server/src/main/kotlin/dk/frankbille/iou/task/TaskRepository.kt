package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TaskRepository : JpaRepository<TaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<TaskEntity>

    fun countByCategoryId(categoryId: Long): Long

    @Query(
        """
        SELECT COUNT(t)
        FROM TaskEntity t
        WHERE t.familyId = :familyId
          AND (t.createdByParent.id = :parentId OR t.updatedByParent.id = :parentId)
        """,
    )
    fun countByFamilyIdAndParentReferences(
        familyId: Long,
        parentId: Long,
    ): Long

    @Query("SELECT t.familyId FROM TaskEntity t WHERE t.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
