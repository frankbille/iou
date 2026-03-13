package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.events.FamilyEventRecorder
import dk.frankbille.iou.events.TaskCategoryChangedEvent
import dk.frankbille.iou.events.TaskCategoryDeletedEvent
import dk.frankbille.iou.security.FamilyScopeCheck
import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import dk.frankbille.iou.task.DeleteTaskCategoryInput
import dk.frankbille.iou.task.TaskRepository
import dk.frankbille.iou.task.UpdateTaskCategoryInput
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaskCategoryService(
    private val taskCategoryRepository: TaskCategoryRepository,
    private val taskRepository: TaskRepository,
    private val familyEventRecorder: FamilyEventRecorder,
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
            .also {
                familyEventRecorder.record(TaskCategoryChangedEvent(it))
            }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskCategoryFamilyId(#input.taskCategoryId)")
    fun updateTaskCategory(input: UpdateTaskCategoryInput): TaskCategory {
        val taskCategory = taskCategoryRepository.findById(input.taskCategoryId).orElseThrow()
        taskCategory.name = input.name.trim()
        return taskCategoryRepository.save(taskCategory).toDto().also {
            familyEventRecorder.record(TaskCategoryChangedEvent(it))
        }
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.taskCategoryFamilyId(#input.taskCategoryId)")
    fun deleteTaskCategory(input: DeleteTaskCategoryInput): Long {
        if (taskRepository.countByCategoryId(input.taskCategoryId) > 0) {
            throw IllegalArgumentException(
                "Cannot delete task category ${input.taskCategoryId} because tasks still reference it",
            )
        }

        val taskCategory = taskCategoryRepository.findById(input.taskCategoryId).orElseThrow()
        taskCategoryRepository.delete(taskCategory)
        return requireNotNull(taskCategory.id).also {
            familyEventRecorder.record(
                TaskCategoryDeletedEvent(
                    familyId = taskCategory.familyId,
                    deletedTaskCategoryId = it,
                ),
            )
        }
    }
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
