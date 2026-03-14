package dk.frankbille.iou.child

import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.security.CurrentViewer
import dk.frankbille.iou.transaction.TransactionService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ChildService(
    private val childRepository: ChildRepository,
    private val currentViewer: CurrentViewer,
    private val transactionService: TransactionService,
) {
    fun getViewerPerson(): Child =
        childRepository
            .findById(currentViewer.childId())
            .map { it.toDto() }
            .orElseThrow { AccessDeniedException("Authenticated child ${currentViewer.childId()} was not found") }

    fun getBalance(childId: Long): Money = Money(transactionService.calculateChildBalance(childId))

    fun getTransactions(childId: Long) = transactionService.getByChildId(childId)
}

fun ChildEntity.toDto(): Child = Child(id = requireNotNull(id), name = name)
