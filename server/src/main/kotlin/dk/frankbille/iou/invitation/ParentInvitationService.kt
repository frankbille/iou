package dk.frankbille.iou.invitation

import dk.frankbille.iou.parent.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ParentInvitationService(
    private val parentInvitationRepository: ParentInvitationRepository,
) {
    fun getByFamilyId(familyId: Long): List<ParentInvitation> =
        parentInvitationRepository.findAllByFamilyIdOrderByCreatedAtDesc(familyId).map { it.toDto() }
}

fun ParentInvitationEntity.toDto(): ParentInvitation =
    ParentInvitation(
        id = requireNotNull(id),
        email = email,
        status = status,
        createdAt = createdAt,
        acceptedAt = acceptedAt,
        expiresAt = expiresAt,
        familyId = familyId,
        invitedBy = invitedByParent.toDto(),
        resolvedParentId = resolvedParent?.id,
    )
