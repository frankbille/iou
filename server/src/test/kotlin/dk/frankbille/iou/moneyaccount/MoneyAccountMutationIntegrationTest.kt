package dk.frankbille.iou.moneyaccount

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.ChildRepository
import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import dk.frankbille.iou.transaction.DepositTransactionEntity
import dk.frankbille.iou.transaction.TransactionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity
import java.time.Instant

class MoneyAccountMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var childRepository: ChildRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var moneyAccountRepository: MoneyAccountRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `createMoneyAccount creates an account for an authorized family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        executeCreateMoneyAccount(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Piggy Bank",
                    "kind" to "SAVINGS",
                ),
        ).path("createMoneyAccount.moneyAccount.name")
            .entity<String>()
            .isEqualTo("Piggy Bank")
            .path("createMoneyAccount.moneyAccount.kind")
            .entity<String>()
            .isEqualTo("SAVINGS")

        assertThat(
            moneyAccountRepository
                .findAllByFamilyIdOrderByNameAsc(requireNotNull(family.id))
                .map(MoneyAccountEntity::name),
        ).containsExactly("Piggy Bank")
    }

    @Test
    fun `createMoneyAccount rejects duplicate names inside a family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Piggy Bank", MoneyAccountKind.CASH))

        executeCreateMoneyAccount(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Piggy Bank",
                    "kind" to "SAVINGS",
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Money account 'Piggy Bank' already exists in family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `createMoneyAccount rejects parents outside the family`() {
        val authorizedParent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), otherParent, relation = "Dad"))

        executeCreateMoneyAccount(
            parentId = requireNotNull(authorizedParent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Piggy Bank",
                    "kind" to "SAVINGS",
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Access Denied",
                    classification = "FORBIDDEN",
                )
            }
    }

    @Test
    fun `updateMoneyAccount updates account fields`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val moneyAccount =
            moneyAccountRepository.save(
                moneyAccount(requireNotNull(family.id), "Piggy Bank", MoneyAccountKind.CASH),
            )

        executeUpdateMoneyAccount(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "moneyAccountId" to requireNotNull(moneyAccount.id),
                    "name" to "Bank Vault",
                    "kind" to "BANK",
                ),
        ).path("updateMoneyAccount.moneyAccount.name")
            .entity<String>()
            .isEqualTo("Bank Vault")
            .path("updateMoneyAccount.moneyAccount.kind")
            .entity<String>()
            .isEqualTo("BANK")

        val updated = moneyAccountRepository.findById(requireNotNull(moneyAccount.id)).orElseThrow()
        assertThat(updated.name).isEqualTo("Bank Vault")
        assertThat(updated.kind).isEqualTo(MoneyAccountKind.BANK)
    }

    @Test
    fun `updateMoneyAccount rejects duplicate names inside a family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val firstAccount =
            moneyAccountRepository.save(
                moneyAccount(requireNotNull(family.id), "Piggy Bank", MoneyAccountKind.CASH),
            )
        moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Bank Vault", MoneyAccountKind.BANK))

        executeUpdateMoneyAccount(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "moneyAccountId" to requireNotNull(firstAccount.id),
                    "name" to "Bank Vault",
                    "kind" to "CASH",
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Money account 'Bank Vault' already exists in family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `deleteMoneyAccount removes unreferenced non default accounts`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val moneyAccount =
            moneyAccountRepository.save(
                moneyAccount(requireNotNull(family.id), "Piggy Bank", MoneyAccountKind.CASH),
            )

        executeDeleteMoneyAccount(
            parentId = requireNotNull(parent.id),
            moneyAccountId = requireNotNull(moneyAccount.id),
        ).path("deleteMoneyAccount.deletedMoneyAccountId")
            .entity<String>()
            .isEqualTo(requireNotNull(moneyAccount.id).toString())

        assertThat(moneyAccountRepository.findById(requireNotNull(moneyAccount.id))).isEmpty()
    }

    @Test
    fun `deleteMoneyAccount rejects the default reward account`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val defaultAccount =
            moneyAccountRepository.save(
                moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH),
            )
        family.defaultRewardAccount = defaultAccount
        familyRepository.save(family)

        executeDeleteMoneyAccount(
            parentId = requireNotNull(parent.id),
            moneyAccountId = requireNotNull(defaultAccount.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot delete money account ${requireNotNull(defaultAccount.id)} because it is the default reward account for family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `deleteMoneyAccount rejects accounts referenced by transactions`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val child = childRepository.save(child(name = "Ava"))
        val moneyAccount =
            moneyAccountRepository.save(
                moneyAccount(requireNotNull(family.id), "Piggy Bank", MoneyAccountKind.CASH),
            )
        transactionRepository.save(
            depositTransaction(
                familyId = requireNotNull(family.id),
                ownerParent = parent,
                child = child,
                account = moneyAccount,
            ),
        )

        executeDeleteMoneyAccount(
            parentId = requireNotNull(parent.id),
            moneyAccountId = requireNotNull(moneyAccount.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot delete money account ${requireNotNull(moneyAccount.id)} because it is referenced by transactions",
                    classification = "BAD_REQUEST",
                )
            }
    }

    private fun executeCreateMoneyAccount(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(createMoneyAccountDocument)
        .variable("input", input)
        .execute()

    private fun executeUpdateMoneyAccount(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateMoneyAccountDocument)
        .variable("input", input)
        .execute()

    private fun executeDeleteMoneyAccount(
        parentId: Long,
        moneyAccountId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(deleteMoneyAccountDocument)
        .variable("input", mapOf("moneyAccountId" to moneyAccountId))
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

private val createMoneyAccountDocument =
    $$"""
    mutation CreateMoneyAccount($input: CreateMoneyAccountInput!) {
      createMoneyAccount(input: $input) {
        moneyAccount {
          id
          name
          kind
        }
      }
    }
    """.trimIndent()

private val updateMoneyAccountDocument =
    $$"""
    mutation UpdateMoneyAccount($input: UpdateMoneyAccountInput!) {
      updateMoneyAccount(input: $input) {
        moneyAccount {
          id
          name
          kind
        }
      }
    }
    """.trimIndent()

private val deleteMoneyAccountDocument =
    $$"""
    mutation DeleteMoneyAccount($input: DeleteMoneyAccountInput!) {
      deleteMoneyAccount(input: $input) {
        deletedMoneyAccountId
      }
    }
    """.trimIndent()
