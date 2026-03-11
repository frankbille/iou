package dk.frankbille.iou.child

import dk.frankbille.iou.moneyaccount.Money
import dk.frankbille.iou.transaction.Transaction
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ChildController(
    private val childService: ChildService,
) {
    @SchemaMapping(typeName = "Child", field = "balance")
    fun balance(child: Child): Money = childService.getBalance(child.id)

    @SchemaMapping(typeName = "Child", field = "transactions")
    fun transactions(child: Child): List<Transaction> = childService.getTransactions(child.id)
}
