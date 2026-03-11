package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository

interface FamilyParentRepository : JpaRepository<FamilyParentEntity, Long> {
    fun findAllByFamilyIdOrderByIdAsc(familyId: Long): List<FamilyParentEntity>

    fun findAllByParentIdOrderByFamilyIdAsc(parentId: Long): List<FamilyParentEntity>

    fun existsByFamilyIdAndParentId(familyId: Long, parentId: Long): Boolean
}
