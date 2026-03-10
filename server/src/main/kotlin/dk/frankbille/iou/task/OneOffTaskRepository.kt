package dk.frankbille.iou.task

import org.springframework.data.jpa.repository.JpaRepository

interface OneOffTaskRepository : JpaRepository<OneOffTaskEntity, Long> {
    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<OneOffTaskEntity>
}
