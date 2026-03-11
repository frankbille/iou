package dk.frankbille.iou.invitation

import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.FamilyAuthorizationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ParentInvitationService(
    private val parentInvitationRepository: ParentInvitationRepository,
    private val familyAuthorizationService: FamilyAuthorizationService,
) {
    fun getByFamilyId(familyId: Long): List<ParentInvitation> {
        familyAuthorizationService.requireAccess(familyId)
        return parentInvitationRepository.findAllByFamilyIdOrderByCreatedAtDesc(familyId).map { it.toDto() }
    }
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
