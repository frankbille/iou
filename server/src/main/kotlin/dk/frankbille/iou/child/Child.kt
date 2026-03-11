package dk.frankbille.iou.child

import dk.frankbille.iou.family.Person
import dk.frankbille.iou.transaction.Transaction

data class Child(
    override val id: Long,
    val name: String,
    val transactions: List<Transaction> = emptyList(),
) : Person
