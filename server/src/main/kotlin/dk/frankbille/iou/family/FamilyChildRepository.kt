package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FamilyChildRepository : JpaRepository<FamilyChildEntity, Long> {
    fun findAllByFamilyIdOrderByIdAsc(familyId: Long): List<FamilyChildEntity>

    fun findByFamilyIdAndChildId(
        familyId: Long,
        childId: Long,
    ): FamilyChildEntity?

    @Query("SELECT fc.child.id FROM FamilyChildEntity fc WHERE fc.familyId = :familyId AND fc.child.id IN :childIds")
    fun findChildIdsByFamilyIdAndChildIds(
        familyId: Long,
        childIds: Collection<Long>,
    ): List<Long>

    @Query("SELECT fc.familyId FROM FamilyChildEntity fc WHERE fc.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
