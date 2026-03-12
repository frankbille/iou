package dk.frankbille.iou.family

import dk.frankbille.iou.invitation.ParentInvitation
import dk.frankbille.iou.invitation.ParentInvitationService
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.moneyaccount.MoneyAccountService
import dk.frankbille.iou.task.Task
import dk.frankbille.iou.taskcategory.TaskCategory
import dk.frankbille.iou.taskcategory.TaskCategoryService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class FamilyController(
    private val familyService: FamilyService,
    private val familyMembershipService: FamilyMembershipService,
    private val parentInvitationService: ParentInvitationService,
    private val moneyAccountService: MoneyAccountService,
    private val taskCategoryService: TaskCategoryService,
    private val taskService: dk.frankbille.iou.task.TaskService,
) {
    @QueryMapping
    fun viewer(): Viewer = familyService.getViewer()

    @MutationMapping
    fun createFamily(
        @Argument @Valid input: CreateFamilyInput,
    ): CreateFamilyPayload = CreateFamilyPayload(familyService.createFamily(input))

    @MutationMapping
    fun updateFamily(
        @Argument @Valid input: UpdateFamilyInput,
    ): UpdateFamilyPayload = UpdateFamilyPayload(familyService.updateFamily(input))

    @MutationMapping
    fun deleteFamily(
        @Argument input: DeleteFamilyInput,
    ): DeleteFamilyPayload = DeleteFamilyPayload(familyService.deleteFamily(input))

    @MutationMapping
    fun inviteParentToFamily(
        @Argument @Valid input: InviteParentToFamilyInput,
    ): InviteParentToFamilyPayload = InviteParentToFamilyPayload(parentInvitationService.inviteParentToFamily(input))

    @MutationMapping
    fun revokeParentInvitation(
        @Argument input: RevokeParentInvitationInput,
    ): RevokeParentInvitationPayload = RevokeParentInvitationPayload(parentInvitationService.revokeParentInvitation(input))

    @MutationMapping
    fun updateFamilyParent(
        @Argument @Valid input: UpdateFamilyParentInput,
    ): UpdateFamilyParentPayload = UpdateFamilyParentPayload(familyMembershipService.updateFamilyParent(input))

    @MutationMapping
    fun removeParentFromFamily(
        @Argument input: RemoveParentFromFamilyInput,
    ): RemoveParentFromFamilyPayload = RemoveParentFromFamilyPayload(familyMembershipService.removeParentFromFamily(input))

    @MutationMapping
    fun addChildToFamily(
        @Argument @Valid input: AddChildToFamilyInput,
    ): AddChildToFamilyPayload = AddChildToFamilyPayload(familyMembershipService.addChildToFamily(input))

    @MutationMapping
    fun updateFamilyChild(
        @Argument @Valid input: UpdateFamilyChildInput,
    ): UpdateFamilyChildPayload = UpdateFamilyChildPayload(familyMembershipService.updateFamilyChild(input))

    @MutationMapping
    fun removeChildFromFamily(
        @Argument input: RemoveChildFromFamilyInput,
    ): RemoveChildFromFamilyPayload = RemoveChildFromFamilyPayload(familyMembershipService.removeChildFromFamily(input))

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
