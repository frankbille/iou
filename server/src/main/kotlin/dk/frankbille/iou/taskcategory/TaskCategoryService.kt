package dk.frankbille.iou.taskcategory

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaskCategoryService(
    private val taskCategoryRepository: TaskCategoryRepository,
) {
    fun getByFamilyId(familyId: Long): List<TaskCategory> =
        taskCategoryRepository.findAllByFamilyIdOrderByNameAsc(familyId).map { it.toDto() }
}

fun TaskCategoryEntity.toDto(): TaskCategory =
    TaskCategory(
        id = requireNotNull(id),
        name = name,
        familyId = familyId,
    )
