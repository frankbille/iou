package dk.frankbille.iou.task

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class TaskController(
    private val familyService: FamilyService,
    private val taskService: TaskService,
) {
    @SchemaMapping(typeName = "OneOffTask", field = "family")
    fun family(task: OneOffTask): Family = familyService.getFamily(task.familyId)

    @SchemaMapping(typeName = "RecurringTask", field = "family")
    fun family(task: RecurringTask): Family = familyService.getFamily(task.familyId)

    @SchemaMapping(typeName = "RecurringTask", field = "completions")
    fun completions(task: RecurringTask): List<RecurringTaskCompletion> = taskService.getCompletions(task.id)
}
