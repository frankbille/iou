package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TaskRepository : JpaRepository<TaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<TaskEntity>

    @Query("SELECT t.familyId FROM TaskEntity t WHERE t.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
