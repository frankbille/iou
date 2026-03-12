package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class MoneyAccountController(
    private val familyService: FamilyService,
    private val moneyAccountService: MoneyAccountService,
) {
    @MutationMapping
    fun createMoneyAccount(
        @Argument @Valid input: CreateMoneyAccountInput,
    ): CreateMoneyAccountPayload = CreateMoneyAccountPayload(moneyAccountService.createMoneyAccount(input))

    @MutationMapping
    fun updateMoneyAccount(
        @Argument @Valid input: UpdateMoneyAccountInput,
    ): UpdateMoneyAccountPayload = UpdateMoneyAccountPayload(moneyAccountService.updateMoneyAccount(input))

    @MutationMapping
    fun deleteMoneyAccount(
        @Argument input: DeleteMoneyAccountInput,
    ): DeleteMoneyAccountPayload = DeleteMoneyAccountPayload(moneyAccountService.deleteMoneyAccount(input))

    @SchemaMapping(typeName = "MoneyAccount", field = "family")
    fun family(account: MoneyAccount): Family = familyService.getFamily(account.familyId)

    @SchemaMapping(typeName = "MoneyAccount", field = "balance")
    fun balance(account: MoneyAccount): Money = moneyAccountService.getBalance(account.id, account.familyId)
}
