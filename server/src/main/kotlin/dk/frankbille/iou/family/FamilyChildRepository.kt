package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FamilyChildRepository : JpaRepository<FamilyChildEntity, Long> {
    fun findAllByFamilyIdOrderByIdAsc(familyId: Long): List<FamilyChildEntity>

    @Query("SELECT fc.familyId FROM FamilyChildEntity fc WHERE fc.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
