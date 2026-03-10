package dk.frankbille.iou.invitation

import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.parent.ParentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "parent_invitations")
class ParentInvitationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    lateinit var family: FamilyEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_parent_id", nullable = false)
    lateinit var invitedByParent: ParentEntity

    @Column(name = "email", length = 320, nullable = false)
    var email: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    var status: ParentInvitationStatus = ParentInvitationStatus.PENDING

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH

    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null

    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_parent_id")
    var resolvedParent: ParentEntity? = null

    @Column(name = "invitation_nonce", nullable = false)
    var invitationNonce: String = ""
}
