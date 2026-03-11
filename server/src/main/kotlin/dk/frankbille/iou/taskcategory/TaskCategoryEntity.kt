package dk.frankbille.iou.taskcategory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
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
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "family_id", nullable = false)
    var familyId: Long = -1

    @Column(name = "name", nullable = false)
    var name: String = ""
}
