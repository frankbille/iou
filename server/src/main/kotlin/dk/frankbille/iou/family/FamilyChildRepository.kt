package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository

interface FamilyChildRepository : JpaRepository<FamilyChildEntity, Long> {
    fun findAllByFamilyIdOrderByIdAsc(familyId: Long): List<FamilyChildEntity>
}
