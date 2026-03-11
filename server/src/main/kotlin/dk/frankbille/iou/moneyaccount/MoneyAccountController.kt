package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class MoneyAccountController(
    private val familyService: FamilyService,
    private val moneyAccountService: MoneyAccountService,
) {
    @SchemaMapping(typeName = "MoneyAccount", field = "family")
    fun family(account: MoneyAccount): Family = familyService.getFamily(account.familyId)

    @SchemaMapping(typeName = "MoneyAccount", field = "balance")
    fun balance(account: MoneyAccount): Money = moneyAccountService.getBalance(account.id, account.familyId)
}
