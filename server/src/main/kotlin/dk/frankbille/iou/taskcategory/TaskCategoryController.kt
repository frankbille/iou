package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class TaskCategoryController(
    private val familyService: FamilyService,
) {
    @SchemaMapping(typeName = "TaskCategory", field = "family")
    fun family(taskCategory: TaskCategory): Family = familyService.getFamily(taskCategory.familyId)
}
