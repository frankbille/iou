package dk.frankbille.iou.transaction

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.parent.ParentEntity
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.Instant.EPOCH

@Entity
@Table(name = "transactions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "transaction_type", discriminatorType = DiscriminatorType.STRING, length = 32)
abstract class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "family_id", nullable = false)
    var familyId: Long = -1

    @Column(name = "timestamp", nullable = false)
    var timestamp: Instant = EPOCH

    @Column(name = "amount_minor", nullable = false)
    var amountMinor: Int = 0

    @Lob
    @Column(name = "description")
    var description: String? = null

    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "owner_parent_id", nullable = false)
    lateinit var ownerParent: ParentEntity

    @ManyToOne(fetch = EAGER, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    lateinit var child: ChildEntity
}
