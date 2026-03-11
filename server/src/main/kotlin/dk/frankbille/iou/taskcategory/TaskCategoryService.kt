package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.security.FamilyAuthorizationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaskCategoryService(
    private val taskCategoryRepository: TaskCategoryRepository,
    private val familyAuthorizationService: FamilyAuthorizationService,
) {
    fun getByFamilyId(familyId: Long): List<TaskCategory> {
        familyAuthorizationService.requireAccess(familyId)
        return taskCategoryRepository.findAllByFamilyIdOrderByNameAsc(familyId).map { it.toDto() }
    }
}

fun TaskCategoryEntity.toDto(): TaskCategory =
    TaskCategory(
        id = requireNotNull(id),
        name = name,
        familyId = familyId,
    )
