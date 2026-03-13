package dk.frankbille.iou.family

import dk.frankbille.iou.child.toDto
import dk.frankbille.iou.events.FamilyDeletedEvent
import dk.frankbille.iou.events.FamilyEventRecorder
import dk.frankbille.iou.events.FamilyUpdatedEvent
import dk.frankbille.iou.moneyaccount.Currency
import dk.frankbille.iou.moneyaccount.CurrencyInput
import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import dk.frankbille.iou.moneyaccount.toDto
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.parent.ParentService
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.CurrentViewer
import dk.frankbille.iou.security.FamilyAuthorizationService
import dk.frankbille.iou.security.HasAccessToFamily
import dk.frankbille.iou.security.HasAccessToFamilyAndIsParent
import dk.frankbille.iou.security.IsParent
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FamilyService(
    private val familyRepository: FamilyRepository,
    private val familyParentRepository: FamilyParentRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val familyAuthorizationService: FamilyAuthorizationService,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val parentRepository: ParentRepository,
    private val currentViewer: CurrentViewer,
    private val parentService: ParentService,
    private val familyEventRecorder: FamilyEventRecorder,
) {
    fun getViewer(): Viewer {
        val accessibleFamilyIds = familyAuthorizationService.getAccessibleFamilyIds()
        val familiesById =
            familyRepository
                .findAllById(accessibleFamilyIds)
                .associateBy { requireNotNull(it.id) }

        return Viewer(
            person = parentService.getViewerPerson(),
            families = accessibleFamilyIds.mapNotNull { familyId -> familiesById[familyId]?.toDto() },
        )
    }

    @HasAccessToFamily
    fun getFamily(familyId: Long): Family = familyRepository.findById(familyId).orElseThrow().toDto()

    @HasAccessToFamily
    fun getFamilyParents(familyId: Long): List<FamilyParent> =
        familyParentRepository.findAllByFamilyIdOrderByIdAsc(familyId).map { it.toDto() }

    @HasAccessToFamily
    fun getFamilyChildren(familyId: Long): List<FamilyChild> =
        familyChildRepository.findAllByFamilyIdOrderByIdAsc(familyId).map { it.toDto() }

    @Transactional
    @IsParent
    fun createFamily(input: CreateFamilyInput): Family {
        val parent = currentParentEntity()
        val family =
            familyRepository.save(
                FamilyEntity().apply {
                    name = input.name.trim()
                    applyCurrency(input.currency)
                    recurringTaskCompletionGracePeriodDays = input.recurringTaskCompletionGracePeriodDays
                },
            )

        val defaultRewardAccount =
            moneyAccountRepository.save(
                MoneyAccountEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = input.defaultRewardAccountName.trim()
                    kind = input.defaultRewardAccountKind
                },
            )

        family.defaultRewardAccount = defaultRewardAccount
        familyParentRepository.save(
            FamilyParentEntity().apply {
                familyId = requireNotNull(family.id)
                this.parent = parent
                relation = "Parent"
            },
        )

        return familyRepository.save(family).toDto()
    }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun updateFamily(input: UpdateFamilyInput): Family {
        val family = familyRepository.findById(input.familyId).orElseThrow()
        val defaultRewardAccount = moneyAccountRepository.findById(input.defaultRewardAccountId).orElseThrow()

        if (defaultRewardAccount.familyId != family.id) {
            throw IllegalArgumentException(
                "Money account ${input.defaultRewardAccountId} does not belong to family ${input.familyId}",
            )
        }

        family.apply {
            name = input.name.trim()
            applyCurrency(input.currency)
            this.defaultRewardAccount = defaultRewardAccount
            recurringTaskCompletionGracePeriodDays = input.recurringTaskCompletionGracePeriodDays
        }

        return familyRepository.save(family).toDto().also {
            familyEventRecorder.record(FamilyUpdatedEvent(it))
        }
    }

    @Transactional
    @HasAccessToFamilyAndIsParent
    fun deleteFamily(input: DeleteFamilyInput): Long {
        val family = familyRepository.findById(input.familyId).orElseThrow()
        family.defaultRewardAccount = null
        familyRepository.saveAndFlush(family)
        familyRepository.delete(family)
        familyRepository.flush()
        return requireNotNull(family.id).also {
            familyEventRecorder.record(
                FamilyDeletedEvent(
                    familyId = it,
                    deletedFamilyId = it,
                ),
            )
        }
    }

    private fun currentParentEntity(): ParentEntity =
        parentRepository
            .findById(currentViewer.parentId())
            .orElseThrow { AccessDeniedException("Authenticated parent ${currentViewer.parentId()} was not found") }

    private fun FamilyEntity.applyCurrency(currency: CurrencyInput) {
        currencyCode = currency.code.trim()
        currencyName = currency.name.trim()
        currencySymbol = currency.symbol.trim()
        currencyPosition = currency.position
        currencyMinorUnit = currency.minorUnit
        currencyKind = currency.kind
    }
}

fun FamilyEntity.toDto(): Family =
    Family(
        id = requireNotNull(id),
        name = name,
        currency =
            Currency(
                code = currencyCode,
                name = currencyName,
                symbol = currencySymbol,
                position = currencyPosition,
                minorUnit = currencyMinorUnit,
                kind = currencyKind,
            ),
        recurringTaskCompletionGracePeriodDays = recurringTaskCompletionGracePeriodDays,
        defaultRewardAccount = defaultRewardAccount?.toDto(),
    )

fun FamilyParentEntity.toDto(): FamilyParent =
    FamilyParent(
        id = requireNotNull(id),
        relation = relation,
        familyId = familyId,
        parent = parent.toDto(),
    )

fun FamilyChildEntity.toDto(): FamilyChild =
    FamilyChild(
        id = requireNotNull(id),
        child = child.toDto(),
        relation = relation,
        rewardPayoutPolicyOverride = rewardPayoutPolicyOverride,
        familyId = familyId,
    )
