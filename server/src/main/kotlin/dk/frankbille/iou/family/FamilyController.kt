package dk.frankbille.iou.family

import dk.frankbille.iou.invitation.ParentInvitation
import dk.frankbille.iou.invitation.ParentInvitationService
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.moneyaccount.MoneyAccountService
import dk.frankbille.iou.task.Task
import dk.frankbille.iou.taskcategory.TaskCategory
import dk.frankbille.iou.taskcategory.TaskCategoryService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class FamilyController(
    private val familyService: FamilyService,
    private val parentInvitationService: ParentInvitationService,
    private val moneyAccountService: MoneyAccountService,
    private val taskCategoryService: TaskCategoryService,
    private val taskService: dk.frankbille.iou.task.TaskService,
) {
    @QueryMapping
    fun viewer(): Viewer = familyService.getViewer()

    @SchemaMapping(typeName = "Family", field = "parents")
    fun parents(family: Family): List<FamilyParent> = familyService.getFamilyParents(family.id)

    @SchemaMapping(typeName = "Family", field = "children")
    fun children(family: Family): List<FamilyChild> = familyService.getFamilyChildren(family.id)

    @SchemaMapping(typeName = "Family", field = "parentInvitations")
    fun parentInvitations(family: Family): List<ParentInvitation> = parentInvitationService.getByFamilyId(family.id)

    @SchemaMapping(typeName = "Family", field = "moneyAccounts")
    fun moneyAccounts(family: Family): List<MoneyAccount> = moneyAccountService.getByFamilyId(family.id)

    @SchemaMapping(typeName = "Family", field = "taskCategories")
    fun taskCategories(family: Family): List<TaskCategory> = taskCategoryService.getByFamilyId(family.id)

    @SchemaMapping(typeName = "Family", field = "tasks")
    fun tasks(family: Family): List<Task> = taskService.getByFamilyId(family.id)

    @SchemaMapping(typeName = "FamilyParent", field = "family")
    fun family(familyParent: FamilyParent): Family = familyService.getFamily(familyParent.familyId)

    @SchemaMapping(typeName = "FamilyChild", field = "family")
    fun family(familyChild: FamilyChild): Family = familyService.getFamily(familyChild.familyId)
}
