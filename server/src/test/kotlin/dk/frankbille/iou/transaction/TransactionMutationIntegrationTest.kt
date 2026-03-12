package dk.frankbille.iou.transaction

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.ChildRepository
import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyChildEntity
import dk.frankbille.iou.family.FamilyChildRepository
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
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

class TransactionMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var childRepository: ChildRepository

    @Autowired
    private lateinit var familyChildRepository: FamilyChildRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var moneyAccountRepository: MoneyAccountRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `recordDeposit creates a deposit transaction`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val account = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet", MoneyAccountKind.CASH))

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(recordDepositDocument)
            .variable(
                "input",
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "childId" to requireNotNull(child.id),
                    "toAccountId" to requireNotNull(account.id),
                    "amountMinor" to 500,
                    "description" to "Birthday money",
                ),
            ).execute()
            .path("recordDeposit.transaction.toAccount.id")
            .entity<String>()
            .isEqualTo(requireNotNull(account.id).toString())
            .path("recordDeposit.transaction.amount.amountMinor")
            .entity<Int>()
            .isEqualTo(500)

        val savedTransaction = transactionRepository.findAll().single() as DepositTransactionEntity
        assertThat(savedTransaction.familyId).isEqualTo(family.id)
        assertThat(savedTransaction.child.id).isEqualTo(child.id)
        assertThat(savedTransaction.accountOne.id).isEqualTo(account.id)
        assertThat(savedTransaction.amountMinor).isEqualTo(500)
    }

    @Test
    fun `recordTransfer rejects using the same account twice`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val account = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet", MoneyAccountKind.CASH))

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(recordTransferDocument)
            .variable(
                "input",
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "childId" to requireNotNull(child.id),
                    "fromAccountId" to requireNotNull(account.id),
                    "toAccountId" to requireNotNull(account.id),
                    "amountMinor" to 200,
                    "description" to "Move funds",
                ),
            ).execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Transfer source and destination accounts must differ",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `recordAdjustment with MANUAL_REMOVE decreases child and account balances`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val account = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet", MoneyAccountKind.CASH))

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(recordAdjustmentDocument)
            .variable(
                "input",
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "childId" to requireNotNull(child.id),
                    "accountId" to requireNotNull(account.id),
                    "amountMinor" to 125,
                    "reason" to "MANUAL_REMOVE",
                    "description" to "Correction",
                ),
            ).execute()

        val families =
            authenticatedGraphQlTester(requireNotNull(parent.id))
                .document(viewerBalancesDocument)
                .execute()
                .path("viewer.families")
                .entityList(FamilyBalancesView::class.java)
                .get()

        val familyView = families.single { it.id == requireNotNull(family.id).toString() }
        val accountView = familyView.moneyAccounts.single { it.id == requireNotNull(account.id).toString() }
        val childView = familyView.children.single { it.child.id == requireNotNull(child.id).toString() }

        assertThat(accountView.balance.amountMinor).isEqualTo(-125)
        assertThat(childView.child.balance.amountMinor).isEqualTo(-125)
    }

    @Test
    fun `recordWithdrawal rejects parents outside the family`() {
        val memberParent = parentRepository.save(parent("Jane Doe"))
        val outsiderParent = parentRepository.save(parent("John Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), memberParent, "Mom"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val account = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet", MoneyAccountKind.CASH))

        authenticatedGraphQlTester(requireNotNull(outsiderParent.id))
            .document(recordWithdrawalDocument)
            .variable(
                "input",
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "childId" to requireNotNull(child.id),
                    "fromAccountId" to requireNotNull(account.id),
                    "amountMinor" to 200,
                    "description" to "Snack money",
                ),
            ).execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Access Denied",
                    classification = "FORBIDDEN",
                )
            }
    }

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
}

private data class FamilyBalancesView(
    val id: String,
    val moneyAccounts: List<MoneyAccountBalanceView>,
    val children: List<FamilyChildBalanceView>,
)

private data class MoneyAccountBalanceView(
    val id: String,
    val balance: MoneyView,
)

private data class FamilyChildBalanceView(
    val child: ChildBalanceView,
)

private data class ChildBalanceView(
    val id: String,
    val balance: MoneyView,
)

private data class MoneyView(
    val amountMinor: Int,
)

private val recordDepositDocument =
    $$"""
    mutation RecordDeposit($input: RecordDepositInput!) {
      recordDeposit(input: $input) {
        transaction {
          id
          amount {
            amountMinor
          }
          toAccount {
            id
          }
        }
      }
    }
    """.trimIndent()

private val recordWithdrawalDocument =
    $$"""
    mutation RecordWithdrawal($input: RecordWithdrawalInput!) {
      recordWithdrawal(input: $input) {
        transaction {
          id
        }
      }
    }
    """.trimIndent()

private val recordTransferDocument =
    $$"""
    mutation RecordTransfer($input: RecordTransferInput!) {
      recordTransfer(input: $input) {
        transaction {
          id
        }
      }
    }
    """.trimIndent()

private val recordAdjustmentDocument =
    $$"""
    mutation RecordAdjustment($input: RecordAdjustmentInput!) {
      recordAdjustment(input: $input) {
        transaction {
          id
        }
      }
    }
    """.trimIndent()

private val viewerBalancesDocument =
    """
    query ViewerBalances {
      viewer {
        families {
          id
          moneyAccounts {
            id
            balance {
              amountMinor
            }
          }
          children {
            child {
              id
              balance {
                amountMinor
              }
            }
          }
        }
      }
    }
    """.trimIndent()
