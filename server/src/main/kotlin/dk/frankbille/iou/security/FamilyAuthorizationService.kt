package dk.frankbille.iou.security

import dk.frankbille.iou.family.FamilyChildRepository
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.CHILD_MODEL_NAME
import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.PARENT_MODEL_NAME
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FamilyAuthorizationService(
    private val familyParentRepository: FamilyParentRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val currentViewer: CurrentViewer,
) {
    fun getAccessibleFamilyIds(): List<Long> = getAccessibleFamilyIdsForViewer(currentViewer.authenticatedViewer().globalId)

    fun getAccessibleFamilyIdsForParent(parentId: Long): List<Long> = familyParentRepository.findFamilyIdByParentId(parentId)

    fun getAccessibleFamilyIdsForChild(childId: Long): List<Long> = familyChildRepository.findFamilyIdByChildId(childId)

    fun getAccessibleFamilyIdsForViewer(globalId: GlobalId): List<Long> =
        when (globalId.modelName) {
            PARENT_MODEL_NAME -> getAccessibleFamilyIdsForParent(globalId.modelId)
            CHILD_MODEL_NAME -> getAccessibleFamilyIdsForChild(globalId.modelId)
            else -> error("Unsupported viewer model: ${globalId.modelName}")
        }
}
