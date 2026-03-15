package dk.frankbille.iou.invitation

import dk.frankbille.iou.events.FamilyEventRecorder
import dk.frankbille.iou.events.ParentInvitationChangedEvent
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
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.time.toKotlinInstant

@Service
@Transactional(readOnly = true)
class ParentInvitationService(
    private val parentInvitationRepository: ParentInvitationRepository,
    private val familyRepository: FamilyRepository,
    private val parentRepository: ParentRepository,
    private val currentViewer: CurrentViewer,
    private val familyEventRecorder: FamilyEventRecorder,
) {
    private val invitationLifetime: Duration = Duration.ofDays(7)

    @Transactional
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<ParentInvitation> =
        parentInvitationRepository
            .findAllByFamilyIdOrderByCreatedAtDesc(familyId)
            .map { reconcileExpiredPendingInvitation(it).toDto() }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun inviteParentToFamily(input: InviteParentToFamilyInput): ParentInvitation {
        familyRepository.findById(input.familyId).orElseThrow()
        val createdAt = Instant.now()

        return parentInvitationRepository
            .save(
                ParentInvitationEntity().apply {
                    familyId = input.familyId
                    invitedByParent = currentParentEntity()
                    email = input.email.trim().lowercase()
                    this.createdAt = createdAt
                    expiresAt = createdAt.plus(invitationLifetime)
                    invitationNonce = UUID.randomUUID().toString()
                },
            ).toDto()
            .also {
                familyEventRecorder.record(ParentInvitationChangedEvent(it))
            }
    }

    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.invitationFamilyId(#input.invitationId)")
    @Transactional(noRollbackFor = [InvitationNotPendingException::class])
    fun revokeParentInvitation(input: RevokeParentInvitationInput): ParentInvitation {
        val invitation = reconcileExpiredPendingInvitation(parentInvitationRepository.findById(input.invitationId).orElseThrow())
        if (invitation.status != ParentInvitationStatus.PENDING) {
            throw InvitationNotPendingException()
        }

        invitation.status = ParentInvitationStatus.REVOKED
        return parentInvitationRepository.save(invitation).toDto().also {
            familyEventRecorder.record(ParentInvitationChangedEvent(it))
        }
    }

    private fun currentParentEntity(): ParentEntity =
        parentRepository
            .findById(currentViewer.parentId())
            .orElseThrow { AccessDeniedException("Authenticated parent ${currentViewer.parentId()} was not found") }

    private fun reconcileExpiredPendingInvitation(invitation: ParentInvitationEntity): ParentInvitationEntity {
        val expiresAt = invitation.expiresAt
        if (invitation.status != ParentInvitationStatus.PENDING || expiresAt == null || expiresAt.isAfter(Instant.now())) {
            return invitation
        }

        invitation.status = ParentInvitationStatus.EXPIRED
        return parentInvitationRepository.save(invitation).also {
            familyEventRecorder.record(ParentInvitationChangedEvent(it.toDto()))
        }
    }
}

fun ParentInvitationEntity.toDto(): ParentInvitation =
    ParentInvitation(
        id = requireNotNull(id),
        email = email,
        status = status,
        createdAt = createdAt.toKotlinInstant(),
        acceptedAt = acceptedAt?.toKotlinInstant(),
        expiresAt = expiresAt?.toKotlinInstant(),
        familyId = familyId,
        invitedBy = invitedByParent.toDto(),
        resolvedParentId = resolvedParent?.id,
    )

private class InvitationNotPendingException : IllegalArgumentException("Only pending invitations can be revoked")
