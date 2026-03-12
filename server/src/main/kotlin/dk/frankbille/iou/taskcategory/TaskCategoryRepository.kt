package dk.frankbille.iou.taskcategory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TaskCategoryRepository : JpaRepository<TaskCategoryEntity, Long> {
    fun findAllByFamilyIdOrderByNameAsc(familyId: Long): List<TaskCategoryEntity>

    fun existsByFamilyIdAndName(familyId: Long, name: String): Boolean

    fun existsByFamilyIdAndNameAndIdNot(
        familyId: Long,
        name: String,
        id: Long,
    ): Boolean

    @Query("SELECT tc.familyId FROM TaskCategoryEntity tc WHERE tc.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
