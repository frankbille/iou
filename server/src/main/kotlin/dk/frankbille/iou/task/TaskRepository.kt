package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository

interface TaskRepository : JpaRepository<TaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<TaskEntity>
}
