package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaskCategoryService(
    private val taskCategoryRepository: TaskCategoryRepository,
) {
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<TaskCategory> =
        taskCategoryRepository.findAllByFamilyIdOrderByNameAsc(familyId).map { it.toDto() }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun createTaskCategory(input: CreateTaskCategoryInput) =
        taskCategoryRepository
            .save(input.toEntity())
            .toDto()
}

fun CreateTaskCategoryInput.toEntity() =
    TaskCategoryEntity().apply {
        familyId = this@toEntity.familyId
        name = this@toEntity.name.trim()
    }

fun TaskCategoryEntity.toDto(): TaskCategory =
    TaskCategory(
        id = requireNotNull(id),
        name = name,
        familyId = familyId,
    )
