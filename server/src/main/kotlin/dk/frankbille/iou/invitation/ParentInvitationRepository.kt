package dk.frankbille.iou.invitation

import org.springframework.data.jpa.repository.JpaRepository

interface ParentInvitationRepository : JpaRepository<ParentInvitationEntity, Long> {
    fun findByInvitationNonce(invitationNonce: String): ParentInvitationEntity?

    fun findAllByFamilyIdOrderByCreatedAtDesc(familyId: Long): List<ParentInvitationEntity>
}
