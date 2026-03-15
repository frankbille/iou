package dk.frankbille.iou.invitation

import dk.frankbille.iou.parent.Parent
import kotlin.time.Instant

data class ParentInvitation(
    val id: Long,
    val familyId: Long,
    val invitedBy: Parent,
    val email: String,
    val status: ParentInvitationStatus,
    val createdAt: Instant,
    val acceptedAt: Instant?,
    val expiresAt: Instant?,
    val resolvedParentId: Long?,
)
