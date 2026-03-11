package dk.frankbille.iou.parent

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ParentService(
    private val parentRepository: ParentRepository,
) {
    fun getViewerPerson(): Parent =
        parentRepository
            .findAll()
            .minByOrNull { it.id ?: Long.MAX_VALUE }
            ?.toDto()
            ?: Parent(id = 0, name = "Viewer") // TODO change once we have security

    fun getParent(id: Long): Parent = parentRepository.findById(id).orElseThrow().toDto()
}

fun ParentEntity.toDto(): Parent = Parent(id = requireNotNull(id), name = name)
