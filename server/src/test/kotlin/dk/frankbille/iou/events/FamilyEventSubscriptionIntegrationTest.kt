package dk.frankbille.iou.events

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
import dk.frankbille.iou.security.TestJwtFactory
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester
import org.springframework.security.core.context.SecurityContextHolder
import reactor.test.StepVerifier
import java.time.Duration

class FamilyEventSubscriptionIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var executionGraphQlService: ExecutionGraphQlService

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var familyChildRepository: FamilyChildRepository

    @Autowired
    private lateinit var childRepository: ChildRepository

    @Autowired
    private lateinit var moneyAccountRepository: MoneyAccountRepository

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `familyEvents emits money account changed events`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val defaultRewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet"))
        family.defaultRewardAccount = defaultRewardAccount
        familyRepository.saveAndFlush(family)
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        authenticateSubscription(parent.id!!, family.id!!)

        val eventFlux =
            subscriptionTester()
                .document(familyEventsSubscription)
                .variable("familyId", family.id!!)
                .executeSubscription()
                .toFlux()

        StepVerifier
            .create(eventFlux)
            .then {
                authenticatedGraphQlTester(parent.id!!)
                    .document(createMoneyAccountDocument)
                    .variable(
                        "input",
                        mapOf(
                            "familyId" to family.id!!,
                            "name" to "Savings Jar",
                            "kind" to "SAVINGS",
                        ),
                    ).execute()
            }.assertNext { response ->
                check(response.path("familyEvents.__typename").entity(String::class.java).get() == "MoneyAccountChangedEvent")
                check(response.path("familyEvents.moneyAccount.name").entity(String::class.java).get() == "Savings Jar")
                check(response.path("familyEvents.moneyAccount.family.id").entity(Long::class.java).get() == family.id!!)
            }.thenCancel()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `familyEvents emits money account deleted events`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val defaultRewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet"))
        val savings = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Savings Jar", kind = MoneyAccountKind.SAVINGS))
        family.defaultRewardAccount = defaultRewardAccount
        familyRepository.saveAndFlush(family)
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        authenticateSubscription(parent.id!!, family.id!!)

        val eventFlux =
            subscriptionTester()
                .document(familyEventsSubscription)
                .variable("familyId", family.id!!)
                .executeSubscription()
                .toFlux()

        StepVerifier
            .create(eventFlux)
            .then {
                authenticatedGraphQlTester(parent.id!!)
                    .document(deleteMoneyAccountDocument)
                    .variable(
                        "input",
                        mapOf("moneyAccountId" to savings.id!!),
                    ).execute()
            }.assertNext { response ->
                check(response.path("familyEvents.__typename").entity(String::class.java).get() == "MoneyAccountDeletedEvent")
                check(response.path("familyEvents.deletedMoneyAccountId").entity(Long::class.java).get() == savings.id!!)
            }.thenCancel()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `familyEvents emits transaction recorded events`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        val defaultRewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Wallet"))
        family.defaultRewardAccount = defaultRewardAccount
        familyRepository.saveAndFlush(family)
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val child = childRepository.save(child(name = "Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, relation = "Daughter"))

        authenticateSubscription(parent.id!!, family.id!!)

        val eventFlux =
            subscriptionTester()
                .document(familyEventsSubscription)
                .variable("familyId", family.id!!)
                .executeSubscription()
                .toFlux()

        StepVerifier
            .create(eventFlux)
            .then {
                authenticatedGraphQlTester(parent.id!!)
                    .document(recordDepositDocument)
                    .variable(
                        "input",
                        mapOf(
                            "familyId" to family.id!!,
                            "childId" to child.id!!,
                            "toAccountId" to defaultRewardAccount.id!!,
                            "amountMinor" to 250,
                            "description" to "Allowance top-up",
                        ),
                    ).execute()
            }.assertNext { response ->
                check(response.path("familyEvents.__typename").entity(String::class.java).get() == "TransactionRecordedEvent")
                check(response.path("familyEvents.transaction.__typename").entity(String::class.java).get() == "DepositTransaction")
                check(response.path("familyEvents.transaction.amount.amountMinor").entity(Int::class.java).get() == 250)
                check(response.path("familyEvents.transaction.child.id").entity(Long::class.java).get() == child.id!!)
            }.thenCancel()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `familyEvents only emits events for the subscribed family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val subscribedFamily = familyRepository.save(family(name = "Family One"))
        val subscribedAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(subscribedFamily.id), "Wallet"))
        subscribedFamily.defaultRewardAccount = subscribedAccount
        familyRepository.saveAndFlush(subscribedFamily)
        familyParentRepository.save(familyParent(requireNotNull(subscribedFamily.id), parent, relation = "Mom"))

        val otherFamily = familyRepository.save(family(name = "Family Two"))
        val otherAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(otherFamily.id), "Wallet"))
        otherFamily.defaultRewardAccount = otherAccount
        familyRepository.saveAndFlush(otherFamily)
        familyParentRepository.save(familyParent(requireNotNull(otherFamily.id), parent, relation = "Mom"))

        authenticateSubscription(parent.id!!, subscribedFamily.id!!)

        val eventFlux =
            subscriptionTester()
                .document(familyEventsSubscription)
                .variable("familyId", subscribedFamily.id!!)
                .executeSubscription()
                .toFlux()

        StepVerifier
            .create(eventFlux)
            .then {
                authenticatedGraphQlTester(parent.id!!)
                    .document(createMoneyAccountDocument)
                    .variable(
                        "input",
                        mapOf(
                            "familyId" to otherFamily.id!!,
                            "name" to "Other Savings",
                            "kind" to "SAVINGS",
                        ),
                    ).execute()
            }.expectNoEvent(Duration.ofMillis(250))
            .thenCancel()
            .verify(Duration.ofSeconds(5))
    }

    private fun authenticateSubscription(
        parentId: Long,
        familyId: Long,
    ) {
        SecurityContextHolder
            .createEmptyContext()
            .apply {
                authentication = TestJwtFactory.createParentAuthentication(parentId, familyIds = listOf(familyId))
            }.also(SecurityContextHolder::setContext)
    }

    private fun subscriptionTester(): ExecutionGraphQlServiceTester = ExecutionGraphQlServiceTester.create(executionGraphQlService)

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
        kind: MoneyAccountKind = MoneyAccountKind.CASH,
    ) = MoneyAccountEntity().apply {
        this.familyId = familyId
        this.name = name
        this.kind = kind
    }
}

private val familyEventsSubscription =
    $$"""
    subscription FamilyEvents($familyId: ID!) {
      familyEvents(familyId: $familyId) {
        __typename
        ... on MoneyAccountChangedEvent {
          moneyAccount {
            id
            name
            family {
              id
            }
          }
        }
        ... on MoneyAccountDeletedEvent {
          deletedMoneyAccountId
        }
        ... on TransactionRecordedEvent {
          transaction {
            __typename
            amount {
              amountMinor
            }
            ... on DepositTransaction {
              child {
                id
              }
            }
          }
        }
      }
    }
    """.trimIndent()

private val createMoneyAccountDocument =
    $$"""
    mutation CreateMoneyAccount($input: CreateMoneyAccountInput!) {
      createMoneyAccount(input: $input) {
        moneyAccount {
          id
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

private val recordDepositDocument =
    $$"""
    mutation RecordDeposit($input: RecordDepositInput!) {
      recordDeposit(input: $input) {
        transaction {
          id
        }
      }
    }
    """.trimIndent()
