package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FamilyParentRepository : JpaRepository<FamilyParentEntity, Long> {
    fun findAllByFamilyIdOrderByIdAsc(familyId: Long): List<FamilyParentEntity>

    fun findAllByParentIdOrderByFamilyIdAsc(parentId: Long): List<FamilyParentEntity>

    @Query("SELECT fp.familyId FROM FamilyParentEntity fp WHERE fp.parent.id = :parentId")
    fun findFamilyIdByParentId(parentId: Long): List<Long>
}
