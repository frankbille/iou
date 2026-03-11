package dk.frankbille.iou.parent

import dk.frankbille.iou.security.CurrentViewer
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ParentService(
    private val parentRepository: ParentRepository,
    private val currentViewer: CurrentViewer,
) {
    fun getViewerPerson(): Parent =
        parentRepository
            .findById(currentViewer.parentId())
            .map { it.toDto() }
            .orElseThrow { AccessDeniedException("Authenticated parent ${currentViewer.parentId()} was not found") }

    fun getParent(id: Long): Parent = parentRepository.findById(id).orElseThrow().toDto()
}

fun ParentEntity.toDto(): Parent = Parent(id = requireNotNull(id), name = name)
