package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class TaskCategoryController(
    private val familyService: FamilyService,
    private val taskCategoryService: TaskCategoryService,
) {
    @MutationMapping
    fun createTaskCategory(
        @Argument @Valid input: CreateTaskCategoryInput,
    ): CreateTaskCategoryPayload = taskCategoryService.createTaskCategory(input).toCreateTaskCategoryPayload()

    @SchemaMapping(typeName = "TaskCategory", field = "family")
    fun family(taskCategory: TaskCategory): Family = familyService.getFamily(taskCategory.familyId)

    private fun TaskCategory.toCreateTaskCategoryPayload() = CreateTaskCategoryPayload(this)
}
