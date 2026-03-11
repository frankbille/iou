package dk.frankbille.iou.child

import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.transaction.TransactionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ChildService(
    private val childRepository: ChildRepository,
    private val transactionService: TransactionService,
) {
    fun getBalance(childId: Long): Money = Money(transactionService.calculateChildBalance(childId))

    fun getTransactions(childId: Long) = transactionService.getByChildId(childId)
}

fun ChildEntity.toDto(): Child = Child(id = requireNotNull(id), name = name)
