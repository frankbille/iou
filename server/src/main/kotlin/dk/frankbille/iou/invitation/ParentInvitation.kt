package dk.frankbille.iou.invitation

import dk.frankbille.iou.parent.Parent
import java.time.OffsetDateTime

data class ParentInvitation(
    val id: Long,
    val familyId: Long,
    val invitedBy: Parent,
    val email: String,
    val status: ParentInvitationStatus,
    val createdAt: OffsetDateTime,
    val acceptedAt: OffsetDateTime?,
    val expiresAt: OffsetDateTime?,
    val resolvedParentId: Long?,
)
