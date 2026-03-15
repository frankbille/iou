package dk.frankbille.iou.family

import dk.frankbille.iou.invitation.ParentInvitation
import dk.frankbille.iou.moneyaccount.CurrencyInput
import dk.frankbille.iou.moneyaccount.MoneyAccountKind
import dk.frankbille.iou.task.RewardPayoutPolicy
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

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
