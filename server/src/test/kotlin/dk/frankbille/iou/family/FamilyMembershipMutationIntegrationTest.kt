package dk.frankbille.iou.family

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.ChildRepository
import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import dk.frankbille.iou.moneyaccount.MoneyAccountKind
import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.task.RewardPayoutPolicy.ON_APPROVAL
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import dk.frankbille.iou.transaction.DepositTransactionEntity
import dk.frankbille.iou.transaction.TransactionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity
import java.time.Instant

class FamilyMembershipMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var childRepository: ChildRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var familyChildRepository: FamilyChildRepository

    @Autowired
    private lateinit var moneyAccountRepository: MoneyAccountRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `updateFamilyParent updates the relation label`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val familyParent = familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        executeUpdateFamilyParent(
            parentId = requireNotNull(parent.id),
            familyParentId = requireNotNull(familyParent.id),
            relation = "Guardian",
        ).path("updateFamilyParent.familyParent.relation")
            .entity<String>()
            .isEqualTo("Guardian")

        assertThat(familyParentRepository.findById(requireNotNull(familyParent.id)).orElseThrow().relation)
            .isEqualTo("Guardian")
    }

    @Test
    fun `removeParentFromFamily removes an unreferenced membership`() {
        val actingParent = parentRepository.save(parent(name = "Jane Doe"))
        val removableParent = parentRepository.save(parent(name = "John Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), actingParent, relation = "Mom"))
        val membership =
            familyParentRepository.save(familyParent(requireNotNull(family.id), removableParent, relation = "Dad"))

        executeRemoveParent(
            parentId = requireNotNull(actingParent.id),
            familyParentId = requireNotNull(membership.id),
        ).path("removeParentFromFamily.removedFamilyParentId")
            .entity<String>()
            .isEqualTo(requireNotNull(membership.id).toString())

        assertThat(familyParentRepository.findById(requireNotNull(membership.id))).isEmpty()
    }

    @Test
    fun `removeParentFromFamily rejects parents still referenced by transactions`() {
        val actingParent = parentRepository.save(parent(name = "Jane Doe"))
        val referencedParent = parentRepository.save(parent(name = "John Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), actingParent, relation = "Mom"))
        val membership =
            familyParentRepository.save(familyParent(requireNotNull(family.id), referencedParent, relation = "Dad"))
        val child = childRepository.save(child(name = "Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, relation = "Daughter"))
        val account =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(family.id),
                    name = "Wallet",
                    kind = MoneyAccountKind.CASH,
                ),
            )
        transactionRepository.save(
            depositTransaction(
                familyId = requireNotNull(family.id),
                ownerParent = referencedParent,
                child = child,
                account = account,
            ),
        )

        executeRemoveParent(
            parentId = requireNotNull(actingParent.id),
            familyParentId = requireNotNull(membership.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot remove parent ${requireNotNull(
                        referencedParent.id,
                    )} from family ${requireNotNull(family.id)} because it is still referenced by family data",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `addChildToFamily creates a child membership`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        executeAddChild(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Ava",
                    "relation" to "Daughter",
                    "rewardPayoutPolicyOverride" to "ON_APPROVAL",
                ),
        ).path("addChildToFamily.familyChild.child.name")
            .entity<String>()
            .isEqualTo("Ava")
            .path("addChildToFamily.familyChild.rewardPayoutPolicyOverride")
            .entity<String>()
            .isEqualTo("ON_APPROVAL")

        val familyChild = familyChildRepository.findAll().single()
        assertThat(familyChild.child.name).isEqualTo("Ava")
        assertThat(familyChild.relation).isEqualTo("Daughter")
        assertThat(familyChild.rewardPayoutPolicyOverride).isEqualTo(ON_APPROVAL)
    }

    @Test
    fun `updateFamilyChild updates the child profile and membership`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val child = childRepository.save(child(name = "Ava"))
        val familyChild =
            familyChildRepository.save(
                familyChild(
                    familyId = requireNotNull(family.id),
                    child = child,
                    relation = "Daughter",
                ),
            )

        executeUpdateChild(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyChildId" to requireNotNull(familyChild.id),
                    "name" to "Avery",
                    "relation" to "Kid",
                    "rewardPayoutPolicyOverride" to "ON_APPROVAL",
                ),
        ).path("updateFamilyChild.familyChild.child.name")
            .entity<String>()
            .isEqualTo("Avery")
            .path("updateFamilyChild.familyChild.relation")
            .entity<String>()
            .isEqualTo("Kid")

        val updatedFamilyChild = familyChildRepository.findById(requireNotNull(familyChild.id)).orElseThrow()
        assertThat(updatedFamilyChild.child.name).isEqualTo("Avery")
        assertThat(updatedFamilyChild.relation).isEqualTo("Kid")
        assertThat(updatedFamilyChild.rewardPayoutPolicyOverride).isEqualTo(ON_APPROVAL)
    }

    @Test
    fun `removeChildFromFamily removes an unreferenced membership`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val child = childRepository.save(child(name = "Ava"))
        val familyChild =
            familyChildRepository.save(
                familyChild(
                    familyId = requireNotNull(family.id),
                    child = child,
                    relation = "Daughter",
                ),
            )

        executeRemoveChild(
            parentId = requireNotNull(parent.id),
            familyChildId = requireNotNull(familyChild.id),
        ).path("removeChildFromFamily.removedFamilyChildId")
            .entity<String>()
            .isEqualTo(requireNotNull(familyChild.id).toString())

        assertThat(familyChildRepository.findById(requireNotNull(familyChild.id))).isEmpty()
    }

    @Test
    fun `removeChildFromFamily rejects children still referenced by transactions`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val child = childRepository.save(child(name = "Ava"))
        val familyChild =
            familyChildRepository.save(
                familyChild(
                    familyId = requireNotNull(family.id),
                    child = child,
                    relation = "Daughter",
                ),
            )
        val account =
            moneyAccountRepository.save(
                moneyAccount(
                    familyId = requireNotNull(family.id),
                    name = "Wallet",
                    kind = MoneyAccountKind.CASH,
                ),
            )
        transactionRepository.save(
            depositTransaction(
                familyId = requireNotNull(family.id),
                ownerParent = parent,
                child = child,
                account = account,
            ),
        )

        executeRemoveChild(
            parentId = requireNotNull(parent.id),
            familyChildId = requireNotNull(familyChild.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot remove child ${requireNotNull(
                        child.id,
                    )} from family ${requireNotNull(family.id)} because it is still referenced by family data",
                    classification = "BAD_REQUEST",
                )
            }
    }

    private fun executeUpdateFamilyParent(
        parentId: Long,
        familyParentId: Long,
        relation: String,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateFamilyParentDocument)
        .variable("input", mapOf("familyParentId" to familyParentId, "relation" to relation))
        .execute()

    private fun executeRemoveParent(
        parentId: Long,
        familyParentId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(removeParentDocument)
        .variable("input", mapOf("familyParentId" to familyParentId))
        .execute()

    private fun executeAddChild(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(addChildDocument)
        .variable("input", input)
        .execute()

    private fun executeUpdateChild(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateChildDocument)
        .variable("input", input)
        .execute()

    private fun executeRemoveChild(
        parentId: Long,
        familyChildId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(removeChildDocument)
        .variable("input", mapOf("familyChildId" to familyChildId))
        .execute()

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

    private fun child(name: String) =
        ChildEntity().apply {
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

    private fun familyChild(
        familyId: Long,
        child: ChildEntity,
        relation: String,
    ) = FamilyChildEntity().apply {
        this.familyId = familyId
        this.child = child
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

    private fun depositTransaction(
        familyId: Long,
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
    ) = DepositTransactionEntity().apply {
        this.familyId = familyId
        timestamp = Instant.parse("2026-01-01T00:00:00Z")
        amountMinor = 500
        this.ownerParent = ownerParent
        this.child = child
        accountOne = account
    }
}

private val updateFamilyParentDocument =
    $$"""
    mutation UpdateFamilyParent($input: UpdateFamilyParentInput!) {
      updateFamilyParent(input: $input) {
        familyParent {
          id
          relation
        }
      }
    }
    """.trimIndent()

private val removeParentDocument =
    $$"""
    mutation RemoveParentFromFamily($input: RemoveParentFromFamilyInput!) {
      removeParentFromFamily(input: $input) {
        removedFamilyParentId
      }
    }
    """.trimIndent()

private val addChildDocument =
    $$"""
    mutation AddChildToFamily($input: AddChildToFamilyInput!) {
      addChildToFamily(input: $input) {
        familyChild {
          id
          relation
          rewardPayoutPolicyOverride
          child {
            id
            name
          }
        }
      }
    }
    """.trimIndent()

private val updateChildDocument =
    $$"""
    mutation UpdateFamilyChild($input: UpdateFamilyChildInput!) {
      updateFamilyChild(input: $input) {
        familyChild {
          id
          relation
          child {
            id
            name
          }
        }
      }
    }
    """.trimIndent()

private val removeChildDocument =
    $$"""
    mutation RemoveChildFromFamily($input: RemoveChildFromFamilyInput!) {
      removeChildFromFamily(input: $input) {
        removedFamilyChildId
      }
    }
    """.trimIndent()
