package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.balance.BalanceCalculator
import dk.frankbille.iou.events.FamilyEventRecorder
import dk.frankbille.iou.events.MoneyAccountChangedEvent
import dk.frankbille.iou.events.MoneyAccountDeletedEvent
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.security.FamilyScopeCheck
import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import dk.frankbille.iou.transaction.TransactionRepository
import dk.frankbille.iou.transaction.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MoneyAccountService(
    private val familyRepository: FamilyRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val transactionRepository: TransactionRepository,
    private val familyEventRecorder: FamilyEventRecorder,
) {
    @HasAccessToFamily
    fun getByFamilyId(familyId: Long): List<MoneyAccount> =
        moneyAccountRepository.findAllByFamilyIdOrderByNameAsc(familyId).map { it.toDto() }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun createMoneyAccount(input: CreateMoneyAccountInput): MoneyAccount {
        familyRepository.findById(input.familyId).orElseThrow()

        return moneyAccountRepository
            .save(
                MoneyAccountEntity().apply {
                    familyId = input.familyId
                    name = input.name.trim()
                    kind = input.kind
                },
            ).toDto()
            .also {
                familyEventRecorder.record(MoneyAccountChangedEvent(it))
            }
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.moneyAccountFamilyId(#input.moneyAccountId)")
    fun updateMoneyAccount(input: UpdateMoneyAccountInput): MoneyAccount {
        val moneyAccount = moneyAccountRepository.findById(input.moneyAccountId).orElseThrow()
        moneyAccount.name = input.name.trim()
        moneyAccount.kind = input.kind
        return moneyAccountRepository.save(moneyAccount).toDto().also {
            familyEventRecorder.record(MoneyAccountChangedEvent(it))
        }
    }

    @Transactional
    @IsParent
    @FamilyScopeCheck("@familyScopeResolver.moneyAccountFamilyId(#input.moneyAccountId)")
    fun deleteMoneyAccount(input: DeleteMoneyAccountInput): Long {
        val moneyAccount = moneyAccountRepository.findById(input.moneyAccountId).orElseThrow()
        val family = familyRepository.findById(moneyAccount.familyId).orElseThrow()
        val moneyAccountId = requireNotNull(moneyAccount.id)

        if (family.defaultRewardAccount?.id == moneyAccountId) {
            throw IllegalArgumentException(
                "Cannot delete money account $moneyAccountId because it is the default reward account for family ${family.id}",
            )
        }

        if (transactionRepository.countReferencesByMoneyAccountId(moneyAccountId) > 0) {
            throw IllegalArgumentException(
                "Cannot delete money account $moneyAccountId because it is referenced by transactions",
            )
        }

        moneyAccountRepository.delete(moneyAccount)
        return moneyAccountId.also {
            familyEventRecorder.record(
                MoneyAccountDeletedEvent(
                    familyId = moneyAccount.familyId,
                    deletedMoneyAccountId = it,
                ),
            )
        }
    }

    @HasAccessToFamily
    fun getBalance(
        accountId: Long,
        familyId: Long,
    ) = BalanceCalculator.calculateMoneyAccountBalance(
        accountId = accountId,
        transactions =
            transactionRepository
                .findAllByFamilyIdOrderByTimestampDesc(familyId)
                .map { it.toDto() },
    )
}

fun MoneyAccountEntity.toDto(): MoneyAccount =
    MoneyAccount(
        id = requireNotNull(id),
        name = name,
        kind = kind,
        familyId = familyId,
    )
