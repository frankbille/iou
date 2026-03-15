package dk.frankbille.iou.auth

import dk.frankbille.iou.parent.ParentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "parent_auth_credentials")
class ParentAuthCredentialEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    lateinit var parent: ParentEntity

    @Column(name = "email", nullable = false, length = 320)
    var email: String = ""

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""
}
