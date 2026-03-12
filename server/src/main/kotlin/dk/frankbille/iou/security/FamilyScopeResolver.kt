package dk.frankbille.iou.security

import dk.frankbille.iou.family.FamilyChildRepository
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.invitation.ParentInvitationRepository
import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import dk.frankbille.iou.task.RecurringTaskCompletionRepository
import dk.frankbille.iou.task.TaskRepository
import dk.frankbille.iou.taskcategory.TaskCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("familyScopeResolver")
@Transactional(readOnly = true)
class FamilyScopeResolver(
    private val taskCategoryRepository: TaskCategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val parentInvitationRepository: ParentInvitationRepository,
    private val familyParentRepository: FamilyParentRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val taskRepository: TaskRepository,
    private val recurringTaskCompletionRepository: RecurringTaskCompletionRepository,
) {
    fun taskCategoryFamilyId(taskCategoryId: Long): Long? = taskCategoryRepository.findFamilyIdById(taskCategoryId)

    fun moneyAccountFamilyId(moneyAccountId: Long): Long? = moneyAccountRepository.findFamilyIdById(moneyAccountId)

    fun invitationFamilyId(invitationId: Long): Long? = parentInvitationRepository.findFamilyIdById(invitationId)

    fun familyParentFamilyId(familyParentId: Long): Long? = familyParentRepository.findFamilyIdById(familyParentId)

    fun familyChildFamilyId(familyChildId: Long): Long? = familyChildRepository.findFamilyIdById(familyChildId)

    fun taskFamilyId(taskId: Long): Long? = taskRepository.findFamilyIdById(taskId)

    fun recurringTaskCompletionFamilyId(completionId: Long): Long? = recurringTaskCompletionRepository.findFamilyIdById(completionId)
}
