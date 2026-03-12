package dk.frankbille.iou.security

import dk.frankbille.iou.family.FamilyParentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FamilyAuthorizationService(
    private val familyParentRepository: FamilyParentRepository,
    private val currentViewer: CurrentViewer,
) {
    fun getAccessibleFamilyIds(): List<Long> =
        familyParentRepository.findAllByParentIdOrderByFamilyIdAsc(currentViewer.parentId()).map { it.familyId }

    fun getAccessibleFamilyIdsForParent(parentId: Long): List<Long> = familyParentRepository.findFamilyIdByParentId(parentId)
}
