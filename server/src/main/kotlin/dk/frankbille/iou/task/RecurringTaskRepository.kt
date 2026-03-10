package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository

interface RecurringTaskRepository : JpaRepository<RecurringTaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<RecurringTaskEntity>
}
