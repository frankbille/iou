package dk.frankbille.iou.taskcategory

import jakarta.validation.constraints.NotBlank

data class TaskCategory(
    val id: Long,
    val familyId: Long,
    val name: String,
)

@UniqueTaskCategoryName
data class CreateTaskCategoryInput(
    val familyId: Long,
    @field:NotBlank(message = "Task category name must not be blank")
    val name: String,
)

data class CreateTaskCategoryPayload(
    val taskCategory: TaskCategory,
)
