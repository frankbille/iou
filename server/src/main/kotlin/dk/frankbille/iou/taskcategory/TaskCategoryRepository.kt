package dk.frankbille.iou.taskcategory

import org.springframework.data.jpa.repository.JpaRepository

interface TaskCategoryRepository : JpaRepository<TaskCategoryEntity, Long> {
    fun findAllByFamilyIdOrderByNameAsc(familyId: Long): List<TaskCategoryEntity>

    fun existsByFamilyIdAndName(familyId: Long, name: String): Boolean
}
