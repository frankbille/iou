package dk.frankbille.iou.invitation

import dk.frankbille.iou.invitation.ParentInvitationStatus.PENDING
import dk.frankbille.iou.parent.ParentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.Instant.EPOCH

@Entity
@Table(name = "parent_invitations")
class ParentInvitationEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "family_id", nullable = false)
    var familyId: Long = -1

    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "invited_by_parent_id", nullable = false)
    lateinit var invitedByParent: ParentEntity

    @Column(name = "email", length = 320, nullable = false)
    var email: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    var status: ParentInvitationStatus = PENDING

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = EPOCH

    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null

    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "resolved_parent_id")
    var resolvedParent: ParentEntity? = null

    @Column(name = "invitation_nonce", nullable = false)
    var invitationNonce: String = ""
}
