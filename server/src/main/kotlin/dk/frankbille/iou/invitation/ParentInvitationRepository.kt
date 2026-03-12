package dk.frankbille.iou.invitation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ParentInvitationRepository : JpaRepository<ParentInvitationEntity, Long> {
    fun findByInvitationNonce(invitationNonce: String): ParentInvitationEntity?

    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<ParentInvitationEntity>

    @Query("SELECT pi.familyId FROM ParentInvitationEntity pi WHERE pi.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
