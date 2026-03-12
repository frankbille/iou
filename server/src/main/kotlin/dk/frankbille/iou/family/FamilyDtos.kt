package dk.frankbille.iou.family

import dk.frankbille.iou.child.Child
import dk.frankbille.iou.invitation.ParentInvitation
import dk.frankbille.iou.moneyaccount.Currency
import dk.frankbille.iou.moneyaccount.CurrencyInput
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.moneyaccount.MoneyAccountKind
import dk.frankbille.iou.parent.Parent
import dk.frankbille.iou.task.RewardPayoutPolicy
import dk.frankbille.iou.task.Task
import dk.frankbille.iou.taskcategory.TaskCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

interface Person {
    val id: Long
}

data class Viewer(
    val person: Person,
    val families: List<Family>,
)

data class Family(
    val id: Long,
    val name: String,
    val currency: Currency,
    val defaultRewardAccount: MoneyAccount? = null,
    val recurringTaskCompletionGracePeriodDays: Int,
    val parents: List<FamilyParent> = emptyList(),
    val children: List<FamilyChild> = emptyList(),
    val parentInvitations: List<ParentInvitation> = emptyList(),
    val moneyAccounts: List<MoneyAccount> = emptyList(),
    val taskCategories: List<TaskCategory> = emptyList(),
    val tasks: List<Task> = emptyList(),
)

data class FamilyParent(
    val id: Long,
    val parent: Parent,
    val relation: String,
    val familyId: Long,
)

data class FamilyChild(
    val id: Long,
    val child: Child,
    val relation: String,
    val rewardPayoutPolicyOverride: RewardPayoutPolicy? = null,
    val familyId: Long,
)

data class CreateFamilyInput(
    @field:NotBlank(message = "Family name must not be blank")
    val name: String,
    @field:Valid
    val currency: CurrencyInput,
    @field:NotBlank(message = "Default reward account name must not be blank")
    val defaultRewardAccountName: String,
    val defaultRewardAccountKind: MoneyAccountKind,
    @field:Min(value = 0, message = "Recurring task completion grace period days must be zero or greater")
    val recurringTaskCompletionGracePeriodDays: Int,
)

data class CreateFamilyPayload(
    val family: Family,
)

@DefaultRewardAccountBelongsToFamily
data class UpdateFamilyInput(
    val familyId: Long,
    @field:NotBlank(message = "Family name must not be blank")
    val name: String,
    @field:Valid
    val currency: CurrencyInput,
    val defaultRewardAccountId: Long,
    @field:Min(value = 0, message = "Recurring task completion grace period days must be zero or greater")
    val recurringTaskCompletionGracePeriodDays: Int,
)

data class UpdateFamilyPayload(
    val family: Family,
)

data class DeleteFamilyInput(
    val familyId: Long,
)

data class DeleteFamilyPayload(
    val deletedFamilyId: Long,
)

data class InviteParentToFamilyInput(
    val familyId: Long,
    @field:NotBlank(message = "Invitation email must not be blank")
    @field:Email(message = "Invitation email must be a valid email address")
    val email: String,
)

data class InviteParentToFamilyPayload(
    val invitation: ParentInvitation,
)

data class RevokeParentInvitationInput(
    val invitationId: Long,
)

data class RevokeParentInvitationPayload(
    val invitation: ParentInvitation,
)

data class UpdateFamilyParentInput(
    val familyParentId: Long,
    val relation: String,
)

data class UpdateFamilyParentPayload(
    val familyParent: FamilyParent,
)

data class RemoveParentFromFamilyInput(
    val familyParentId: Long,
)

data class RemoveParentFromFamilyPayload(
    val removedFamilyParentId: Long,
)

data class AddChildToFamilyInput(
    val familyId: Long,
    @field:NotBlank(message = "Child name must not be blank")
    val name: String,
    val relation: String,
    val rewardPayoutPolicyOverride: RewardPayoutPolicy? = null,
)

data class AddChildToFamilyPayload(
    val familyChild: FamilyChild,
)

data class UpdateFamilyChildInput(
    val familyChildId: Long,
    @field:NotBlank(message = "Child name must not be blank")
    val name: String,
    val relation: String,
    val rewardPayoutPolicyOverride: RewardPayoutPolicy? = null,
)

data class UpdateFamilyChildPayload(
    val familyChild: FamilyChild,
)

data class RemoveChildFromFamilyInput(
    val familyChildId: Long,
)

data class RemoveChildFromFamilyPayload(
    val removedFamilyChildId: Long,
)
