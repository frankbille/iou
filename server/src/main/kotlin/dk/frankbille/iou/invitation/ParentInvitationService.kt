package dk.frankbille.iou.invitation

import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.family.InviteParentToFamilyInput
import dk.frankbille.iou.family.RevokeParentInvitationInput
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.CurrentViewer
import dk.frankbille.iou.security.FamilyScopeCheck
import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ParentInvitationService(
    private val parentInvitationRepository: ParentInvitationRepository,
    private val familyRepository: FamilyRepository,
    private val parentRepository: ParentRepository,
    private val currentViewer: CurrentViewer,
) {
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<ParentInvitation> =
        parentInvitationRepository.findAllByFamilyIdOrderByCreatedAtDesc(familyId).map { it.toDto() }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun inviteParentToFamily(input: InviteParentToFamilyInput): ParentInvitation {
        familyRepository.findById(input.familyId).orElseThrow()

        return parentInvitationRepository
            .save(
                ParentInvitationEntity().apply {
                    familyId = input.familyId
                    invitedByParent = currentParentEntity()
                    email = input.email.trim().lowercase()
                    createdAt = Instant.now()
                    invitationNonce = UUID.randomUUID().toString()
                },
            ).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.invitationFamilyId(#input.invitationId)")
    fun revokeParentInvitation(input: RevokeParentInvitationInput): ParentInvitation {
        val invitation = parentInvitationRepository.findById(input.invitationId).orElseThrow()
        if (invitation.status != ParentInvitationStatus.PENDING) {
            throw IllegalArgumentException("Only pending invitations can be revoked")
        }

        invitation.status = ParentInvitationStatus.REVOKED
        return parentInvitationRepository.save(invitation).toDto()
    }

    private fun currentParentEntity(): ParentEntity =
        parentRepository
            .findById(currentViewer.parentId())
            .orElseThrow { AccessDeniedException("Authenticated parent ${currentViewer.parentId()} was not found") }
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
