package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository

interface FamilyParentRepository : JpaRepository<FamilyParentEntity, Long> {
    fun findAllByFamilyIdOrderByIdAsc(familyId: Long): List<FamilyParentEntity>
}
