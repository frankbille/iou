package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.family.FamilyEntity
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
    name = "task_categories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_task_categories_family_name",
            columnNames = ["family_id", "name"],
        ),
    ],
)
class TaskCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    lateinit var family: FamilyEntity

    @Column(name = "name", nullable = false)
    var name: String = ""
}
