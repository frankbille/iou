package dk.frankbille.iou.events

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyChild
import dk.frankbille.iou.family.FamilyParent
import dk.frankbille.iou.invitation.ParentInvitation
import dk.frankbille.iou.moneyaccount.MoneyAccount
import dk.frankbille.iou.task.OneOffTask
import dk.frankbille.iou.task.RecurringTask
import dk.frankbille.iou.task.RecurringTaskCompletion
import dk.frankbille.iou.taskcategory.TaskCategory
import dk.frankbille.iou.transaction.Transaction

sealed interface FamilyScopedEvent {
    val familyId: Long
}

data class FamilyUpdatedEvent(
    val family: Family,
) : FamilyScopedEvent {
    override val familyId: Long = family.id
}

data class FamilyDeletedEvent(
    override val familyId: Long,
    val deletedFamilyId: Long,
) : FamilyScopedEvent

data class ParentInvitationChangedEvent(
    val invitation: ParentInvitation,
) : FamilyScopedEvent {
    override val familyId: Long = invitation.familyId
}

data class FamilyParentChangedEvent(
    val familyParent: FamilyParent,
) : FamilyScopedEvent {
    override val familyId: Long = familyParent.familyId
}

data class FamilyParentRemovedEvent(
    override val familyId: Long,
    val removedFamilyParentId: Long,
) : FamilyScopedEvent

data class FamilyChildChangedEvent(
    val familyChild: FamilyChild,
) : FamilyScopedEvent {
    override val familyId: Long = familyChild.familyId
}

data class FamilyChildRemovedEvent(
    override val familyId: Long,
    val removedFamilyChildId: Long,
) : FamilyScopedEvent

data class MoneyAccountChangedEvent(
    val moneyAccount: MoneyAccount,
) : FamilyScopedEvent {
    override val familyId: Long = moneyAccount.familyId
}

data class MoneyAccountDeletedEvent(
    override val familyId: Long,
    val deletedMoneyAccountId: Long,
) : FamilyScopedEvent

data class TaskCategoryChangedEvent(
    val taskCategory: TaskCategory,
) : FamilyScopedEvent {
    override val familyId: Long = taskCategory.familyId
}

data class TaskCategoryDeletedEvent(
    override val familyId: Long,
    val deletedTaskCategoryId: Long,
) : FamilyScopedEvent

data class OneOffTaskChangedEvent(
    val task: OneOffTask,
) : FamilyScopedEvent {
    override val familyId: Long = task.familyId
}

data class OneOffTaskDeletedEvent(
    override val familyId: Long,
    val deletedTaskId: Long,
) : FamilyScopedEvent

data class RecurringTaskChangedEvent(
    val task: RecurringTask,
) : FamilyScopedEvent {
    override val familyId: Long = task.familyId
}

data class RecurringTaskDeletedEvent(
    override val familyId: Long,
    val deletedTaskId: Long,
) : FamilyScopedEvent

data class RecurringTaskCompletionChangedEvent(
    override val familyId: Long,
    val completion: RecurringTaskCompletion,
) : FamilyScopedEvent

data class TransactionRecordedEvent(
    val transaction: Transaction,
) : FamilyScopedEvent {
    override val familyId: Long = transaction.familyId
}
