package dk.frankbille.iou.taskcategory

import jakarta.validation.constraints.NotBlank

@UniqueTaskCategoryName
data class CreateTaskCategoryInput(
    val familyId: Long,
    @field:NotBlank(message = "Task category name must not be blank")
    val name: String,
)

data class CreateTaskCategoryPayload(
    val taskCategory: TaskCategory,
)
