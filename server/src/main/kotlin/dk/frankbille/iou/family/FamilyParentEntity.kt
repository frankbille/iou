package dk.frankbille.iou.family

import dk.frankbille.iou.parent.ParentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "family_parents",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_family_parents_family_parent",
            columnNames = ["family_id", "parent_id"],
        ),
    ],
)
class FamilyParentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    lateinit var family: FamilyEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    lateinit var parent: ParentEntity

    @Column(name = "relation", nullable = false)
    var relation: String = ""
}
