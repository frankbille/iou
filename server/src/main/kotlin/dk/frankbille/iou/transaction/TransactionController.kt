package dk.frankbille.iou.transaction

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class TransactionController(
    private val familyService: FamilyService,
) {
    @SchemaMapping(typeName = "RewardTransaction", field = "family")
    fun family(transaction: RewardTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "TransferTransaction", field = "family")
    fun family(transaction: TransferTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "AdjustmentTransaction", field = "family")
    fun family(transaction: AdjustmentTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "WithdrawalTransaction", field = "family")
    fun family(transaction: WithdrawalTransaction): Family = familyService.getFamily(transaction.familyId)

    @SchemaMapping(typeName = "DepositTransaction", field = "family")
    fun family(transaction: DepositTransaction): Family = familyService.getFamily(transaction.familyId)
}
