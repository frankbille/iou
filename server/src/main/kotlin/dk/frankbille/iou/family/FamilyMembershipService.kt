package dk.frankbille.iou.family

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.ChildRepository
import dk.frankbille.iou.invitation.ParentInvitationRepository
import dk.frankbille.iou.security.FamilyScopeCheck
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import dk.frankbille.iou.task.OneOffTaskRepository
import dk.frankbille.iou.task.RecurringTaskCompletionRepository
import dk.frankbille.iou.task.RecurringTaskRepository
import dk.frankbille.iou.task.TaskRepository
import dk.frankbille.iou.transaction.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FamilyMembershipService(
    private val familyRepository: FamilyRepository,
    private val familyParentRepository: FamilyParentRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val childRepository: ChildRepository,
    private val parentInvitationRepository: ParentInvitationRepository,
    private val taskRepository: TaskRepository,
    private val oneOffTaskRepository: OneOffTaskRepository,
    private val recurringTaskRepository: RecurringTaskRepository,
    private val recurringTaskCompletionRepository: RecurringTaskCompletionRepository,
    private val transactionRepository: TransactionRepository,
) {
    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.familyParentFamilyId(#input.familyParentId)")
    fun updateFamilyParent(input: UpdateFamilyParentInput): FamilyParent {
        val familyParent = familyParentRepository.findById(input.familyParentId).orElseThrow()
        familyParent.relation = input.relation.trim()
        return familyParentRepository.save(familyParent).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.familyParentFamilyId(#input.familyParentId)")
    fun removeParentFromFamily(input: RemoveParentFromFamilyInput): Long {
        val familyParent = familyParentRepository.findById(input.familyParentId).orElseThrow()
        val familyId = familyParent.familyId
        val parentId = requireNotNull(familyParent.parent.id)

        if (hasParentReferences(familyId, parentId)) {
            throw IllegalArgumentException(
                "Cannot remove parent $parentId from family $familyId because it is still referenced by family data",
            )
        }

        familyParentRepository.delete(familyParent)
        return requireNotNull(familyParent.id)
    }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun addChildToFamily(input: AddChildToFamilyInput): FamilyChild {
        val family = familyRepository.findById(input.familyId).orElseThrow()
        val child =
            childRepository.save(
                ChildEntity().apply {
                    name = input.name.trim()
                },
            )

        return familyChildRepository
            .save(
                FamilyChildEntity().apply {
                    familyId = requireNotNull(family.id)
                    this.child = child
                    relation = input.relation.trim()
                    rewardPayoutPolicyOverride = input.rewardPayoutPolicyOverride
                },
            ).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.familyChildFamilyId(#input.familyChildId)")
    fun updateFamilyChild(input: UpdateFamilyChildInput): FamilyChild {
        val familyChild = familyChildRepository.findById(input.familyChildId).orElseThrow()
        familyChild.child.name = input.name.trim()
        familyChild.relation = input.relation.trim()
        familyChild.rewardPayoutPolicyOverride = input.rewardPayoutPolicyOverride

        childRepository.save(familyChild.child)
        return familyChildRepository.save(familyChild).toDto()
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.familyChildFamilyId(#input.familyChildId)")
    fun removeChildFromFamily(input: RemoveChildFromFamilyInput): Long {
        val familyChild = familyChildRepository.findById(input.familyChildId).orElseThrow()
        val familyId = familyChild.familyId
        val childId = requireNotNull(familyChild.child.id)

        if (hasChildReferences(familyId, childId)) {
            throw IllegalArgumentException(
                "Cannot remove child $childId from family $familyId because it is still referenced by family data",
            )
        }

        familyChildRepository.delete(familyChild)
        return requireNotNull(familyChild.id)
    }

    private fun hasParentReferences(
        familyId: Long,
        parentId: Long,
    ): Boolean =
        taskRepository.countByFamilyIdAndParentReferences(familyId, parentId) > 0 ||
            oneOffTaskRepository.countByFamilyIdAndApprovedByParentId(familyId, parentId) > 0 ||
            recurringTaskCompletionRepository.countByFamilyIdAndApprovedByParentId(familyId, parentId) > 0 ||
            transactionRepository.countByFamilyIdAndOwnerParentId(familyId, parentId) > 0 ||
            parentInvitationRepository.countByFamilyIdAndInvitedByParentId(familyId, parentId) > 0

    private fun hasChildReferences(
        familyId: Long,
        childId: Long,
    ): Boolean =
        oneOffTaskRepository.countByFamilyIdAndChildReferences(familyId, childId) > 0 ||
            recurringTaskRepository.countByFamilyIdAndEligibleChildId(familyId, childId) > 0 ||
            recurringTaskCompletionRepository.countByFamilyIdAndChildId(familyId, childId) > 0 ||
            transactionRepository.countByFamilyIdAndChildId(familyId, childId) > 0
}
