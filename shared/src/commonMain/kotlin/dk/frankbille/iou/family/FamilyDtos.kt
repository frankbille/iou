package dk.frankbille.iou.family

import dk.frankbille.iou.child.Child
import dk.frankbille.iou.invitation.ParentInvitation
import dk.frankbille.iou.moneyaccount.Currency
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.parent.Parent
import dk.frankbille.iou.task.RewardPayoutPolicy
import dk.frankbille.iou.task.Task
import dk.frankbille.iou.taskcategory.TaskCategory

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
