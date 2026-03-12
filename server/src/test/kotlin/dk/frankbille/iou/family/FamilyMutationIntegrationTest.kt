package dk.frankbille.iou.family

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import dk.frankbille.iou.moneyaccount.MoneyAccountKind
import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity

class FamilyMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var moneyAccountRepository: MoneyAccountRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Test
    fun `createFamily creates family default account and creator membership`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))

        executeCreateFamily(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "name" to "Weekend Crew",
                    "currency" to currencyInput(code = "USD", name = "US Dollar", symbol = "$", minorUnit = 2),
                    "defaultRewardAccountName" to "Allowance Wallet",
                    "defaultRewardAccountKind" to "CASH",
                    "recurringTaskCompletionGracePeriodDays" to 4,
                ),
        ).path("createFamily.family.name")
            .entity<String>()
            .isEqualTo("Weekend Crew")
            .path("createFamily.family.defaultRewardAccount.name")
            .entity<String>()
            .isEqualTo("Allowance Wallet")
            .path("createFamily.family.recurringTaskCompletionGracePeriodDays")
            .entity<Int>()
            .isEqualTo(4)

        val family = familyRepository.findAll().single()
        assertThat(family.name).isEqualTo("Weekend Crew")
        assertThat(requireNotNull(family.defaultRewardAccount).name).isEqualTo("Allowance Wallet")
        assertThat(
            familyParentRepository
                .findAllByFamilyIdOrderByIdAsc(requireNotNull(family.id))
                .single()
                .parent
                .id,
        ).isEqualTo(parent.id)
    }

    @Test
    fun `createFamily rejects blank family names`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))

        executeCreateFamily(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "name" to "   ",
                    "currency" to currencyInput(code = "USD", name = "US Dollar", symbol = "$", minorUnit = 2),
                    "defaultRewardAccountName" to "Allowance Wallet",
                    "defaultRewardAccountKind" to "CASH",
                    "recurringTaskCompletionGracePeriodDays" to 4,
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Family name must not be blank",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `updateFamily updates configuration for an authorized family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val originalAccount =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(family.id),
                    name = "Original",
                    kind = MoneyAccountKind.CASH,
                ),
            )
        family.defaultRewardAccount = originalAccount
        familyRepository.save(family)
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val replacementAccount =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(family.id),
                    name = "Bank",
                    kind = MoneyAccountKind.BANK,
                ),
            )

        executeUpdateFamily(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Family Two",
                    "currency" to currencyInput(code = "EUR", name = "Euro", symbol = "€", minorUnit = 2),
                    "defaultRewardAccountId" to requireNotNull(replacementAccount.id),
                    "recurringTaskCompletionGracePeriodDays" to 7,
                ),
        ).path("updateFamily.family.name")
            .entity<String>()
            .isEqualTo("Family Two")
            .path("updateFamily.family.defaultRewardAccount.id")
            .entity<String>()
            .isEqualTo(requireNotNull(replacementAccount.id).toString())

        val updatedFamily = familyRepository.findById(requireNotNull(family.id)).orElseThrow()
        assertThat(updatedFamily.name).isEqualTo("Family Two")
        assertThat(updatedFamily.currencyCode).isEqualTo("EUR")
        assertThat(requireNotNull(updatedFamily.defaultRewardAccount).id).isEqualTo(replacementAccount.id)
        assertThat(updatedFamily.recurringTaskCompletionGracePeriodDays).isEqualTo(7)
    }

    @Test
    fun `updateFamily rejects default reward account from another family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val originalAccount =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(family.id),
                    name = "Original",
                    kind = MoneyAccountKind.CASH,
                ),
            )
        family.defaultRewardAccount = originalAccount
        familyRepository.save(family)
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        val otherFamily = familyRepository.save(family(name = "Family Two"))
        val foreignAccount =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(otherFamily.id),
                    name = "Foreign",
                    kind = MoneyAccountKind.BANK,
                ),
            )

        executeUpdateFamily(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Family One",
                    "currency" to currencyInput(code = "USD", name = "US Dollar", symbol = "$", minorUnit = 2),
                    "defaultRewardAccountId" to requireNotNull(foreignAccount.id),
                    "recurringTaskCompletionGracePeriodDays" to 3,
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Money account ${requireNotNull(foreignAccount.id)} does not belong to family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `deleteFamily removes the family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val defaultAccount =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(family.id),
                    name = "Allowance Wallet",
                    kind = MoneyAccountKind.CASH,
                ),
            )
        family.defaultRewardAccount = defaultAccount
        familyRepository.save(family)
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        executeDeleteFamily(
            parentId = requireNotNull(parent.id),
            familyId = requireNotNull(family.id),
        ).path("deleteFamily.deletedFamilyId")
            .entity<String>()
            .isEqualTo(requireNotNull(family.id).toString())

        assertThat(familyRepository.findAll()).isEmpty()
        assertThat(moneyAccountRepository.findAll()).isEmpty()
        assertThat(familyParentRepository.findAll()).isEmpty()
    }

    private fun executeCreateFamily(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(createFamilyDocument)
        .variable("input", input)
        .execute()

    private fun executeUpdateFamily(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateFamilyDocument)
        .variable("input", input)
        .execute()

    private fun executeDeleteFamily(
        parentId: Long,
        familyId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(deleteFamilyDocument)
        .variable("input", mapOf("familyId" to familyId))
        .execute()

    private fun currencyInput(
        code: String,
        name: String,
        symbol: String,
        minorUnit: Int,
    ) = mapOf(
        "code" to code,
        "name" to name,
        "symbol" to symbol,
        "position" to "PREFIX",
        "minorUnit" to minorUnit,
        "kind" to "ISO_CURRENCY",
    )

    private fun assertGraphQlError(
        error: ResponseError,
        message: String,
        classification: String,
    ) {
        assertThat(error.message).isEqualTo(message)
        assertThat(error.extensions.get("classification")).isEqualTo(classification)
    }

    private fun parent(name: String) =
        ParentEntity().apply {
            this.name = name
        }

    private fun family(name: String) =
        FamilyEntity().apply {
            this.name = name
            currencyCode = "USD"
            currencyName = "US Dollar"
            currencySymbol = "$"
            currencyPosition = PREFIX
            currencyMinorUnit = 2
            currencyKind = ISO_CURRENCY
            recurringTaskCompletionGracePeriodDays = 3
        }

    private fun familyParent(
        familyId: Long,
        parent: ParentEntity,
        relation: String,
    ) = FamilyParentEntity().apply {
        this.familyId = familyId
        this.parent = parent
        this.relation = relation
    }

    private fun moneyAccount(
        familyId: Long,
        name: String,
        kind: MoneyAccountKind,
    ) = MoneyAccountEntity().apply {
        this.familyId = familyId
        this.name = name
        this.kind = kind
    }
}

private val createFamilyDocument =
    $$"""
    mutation CreateFamily($input: CreateFamilyInput!) {
      createFamily(input: $input) {
        family {
          id
          name
          recurringTaskCompletionGracePeriodDays
          defaultRewardAccount {
            id
            name
          }
        }
      }
    }
    """.trimIndent()

private val updateFamilyDocument =
    $$"""
    mutation UpdateFamily($input: UpdateFamilyInput!) {
      updateFamily(input: $input) {
        family {
          id
          name
          defaultRewardAccount {
            id
          }
        }
      }
    }
    """.trimIndent()

private val deleteFamilyDocument =
    $$"""
    mutation DeleteFamily($input: DeleteFamilyInput!) {
      deleteFamily(input: $input) {
        deletedFamilyId
      }
    }
    """.trimIndent()
