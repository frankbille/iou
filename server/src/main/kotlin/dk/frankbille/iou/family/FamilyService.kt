package dk.frankbille.iou.family

import dk.frankbille.iou.child.toDto
import dk.frankbille.iou.moneyaccount.Currency
import dk.frankbille.iou.moneyaccount.toDto
import dk.frankbille.iou.parent.ParentService
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.FamilyAuthorizationService
import dk.frankbille.iou.security.HasAccessToFamily
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FamilyService(
    private val familyRepository: FamilyRepository,
    private val familyParentRepository: FamilyParentRepository,
    private val familyChildRepository: FamilyChildRepository,
    private val familyAuthorizationService: FamilyAuthorizationService,
    private val parentService: ParentService,
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
