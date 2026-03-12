package dk.frankbille.iou.task

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
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import dk.frankbille.iou.taskcategory.TaskCategoryRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import dk.frankbille.iou.transaction.RewardTransactionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class RecurringTaskCompletionMutationIntegrationTest : GraphQlControllerIntegrationTest() {
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
    private lateinit var recurringTaskCompletionRepository: RecurringTaskCompletionRepository

    @Autowired
    private lateinit var recurringTaskRepository: RecurringTaskRepository

    @Autowired
    private lateinit var rewardTransactionRepository: RewardTransactionRepository

    @Autowired
    private lateinit var taskCategoryRepository: TaskCategoryRepository

    @Test
    fun `completeRecurringTask resolves the latest eligible occurrence when occurrenceDate is omitted`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One", recurringTaskCompletionGracePeriodDays = 2))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val rewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH))
        family.defaultRewardAccount = rewardAccount
        familyRepository.save(family)
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val latestEligibleDate = LocalDate.now().minusDays(1)
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
                    daysOfWeek = setOf(latestEligibleDate.dayOfWeek),
                    startsOn = latestEligibleDate.minusWeeks(3),
                    endsOn = latestEligibleDate.plusWeeks(3),
                    maxCompletionsPerPeriod = 2,
                ),
            )

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(completeRecurringTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id), "childId" to requireNotNull(child.id)))
            .execute()
            .path("completeRecurringTask.completion.occurrenceDate")
            .entity<String>()
            .isEqualTo(latestEligibleDate.toString())
            .path("completeRecurringTask.rewardTransaction.taskCompletion.__typename")
            .entity<String>()
            .isEqualTo("RecurringTaskCompletion")

        val completion = recurringTaskCompletionRepository.findAll().single()
        assertThat(completion.occurrenceDate).isEqualTo(latestEligibleDate)
        assertThat(rewardTransactionRepository.findByRecurringTaskCompletionId(requireNotNull(completion.id))).isNotNull
    }

    @Test
    fun `completeRecurringTask enforces maxCompletionsPerPeriod`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val firstChild = childRepository.save(child("Ava"))
        val secondChild = childRepository.save(child("Liam"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), firstChild, "Daughter"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), secondChild, "Son"))
        val occurrenceDate = LocalDate.now()
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_APPROVAL,
                    daysOfWeek = setOf(occurrenceDate.dayOfWeek),
                    startsOn = occurrenceDate.minusWeeks(3),
                    endsOn = occurrenceDate.plusWeeks(3),
                    maxCompletionsPerPeriod = 1,
                ),
            )
        recurringTaskCompletionRepository.save(
            recurringCompletion(task = task, child = firstChild, occurrenceDate = occurrenceDate),
        )

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(completeRecurringTaskDocument)
            .variable(
                "input",
                mapOf(
                    "taskId" to requireNotNull(task.id),
                    "childId" to requireNotNull(secondChild.id),
                    "occurrenceDate" to occurrenceDate.toString(),
                ),
            ).execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Recurring task ${requireNotNull(task.id)} has reached its completion limit for $occurrenceDate",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `approveRecurringTaskCompletion creates reward transaction for ON_APPROVAL`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val rewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH))
        family.defaultRewardAccount = rewardAccount
        familyRepository.save(family)
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val occurrenceDate = LocalDate.now()
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_APPROVAL,
                    daysOfWeek = setOf(occurrenceDate.dayOfWeek),
                    startsOn = occurrenceDate.minusWeeks(3),
                    endsOn = occurrenceDate.plusWeeks(3),
                    maxCompletionsPerPeriod = 2,
                ),
            )

        val completionId =
            authenticatedGraphQlTester(requireNotNull(parent.id))
                .document(completeRecurringTaskDocument)
                .variable(
                    "input",
                    mapOf(
                        "taskId" to requireNotNull(task.id),
                        "childId" to requireNotNull(child.id),
                        "occurrenceDate" to occurrenceDate.toString(),
                    ),
                ).execute()
                .path("completeRecurringTask.completion.id")
                .entity<String>()
                .get()
                .toLong()

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(approveRecurringTaskCompletionDocument)
            .variable("input", mapOf("completionId" to completionId))
            .execute()
            .path("approveRecurringTaskCompletion.completion.status")
            .entity<String>()
            .isEqualTo("APPROVED")
            .path("approveRecurringTaskCompletion.rewardTransaction.taskCompletion.__typename")
            .entity<String>()
            .isEqualTo("RecurringTaskCompletion")

        val savedCompletion = recurringTaskCompletionRepository.findById(completionId).orElseThrow()
        assertThat(savedCompletion.status).isEqualTo(TaskCompletionStatus.APPROVED)
        assertThat(savedCompletion.approvedByParent?.id).isEqualTo(parent.id)
        assertThat(rewardTransactionRepository.findByRecurringTaskCompletionId(completionId)).isNotNull
    }

    @Test
    fun `resetRecurringTaskCompletionToAvailable rejects completions with reward transactions`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val rewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH))
        family.defaultRewardAccount = rewardAccount
        familyRepository.save(family)
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val occurrenceDate = LocalDate.now()
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
                    daysOfWeek = setOf(occurrenceDate.dayOfWeek),
                    startsOn = occurrenceDate.minusWeeks(3),
                    endsOn = occurrenceDate.plusWeeks(3),
                    maxCompletionsPerPeriod = 2,
                ),
            )

        val completionId =
            authenticatedGraphQlTester(requireNotNull(parent.id))
                .document(completeRecurringTaskDocument)
                .variable(
                    "input",
                    mapOf(
                        "taskId" to requireNotNull(task.id),
                        "childId" to requireNotNull(child.id),
                        "occurrenceDate" to occurrenceDate.toString(),
                    ),
                ).execute()
                .path("completeRecurringTask.completion.id")
                .entity<String>()
                .get()
                .toLong()

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(resetRecurringTaskCompletionToAvailableDocument)
            .variable("input", mapOf("completionId" to completionId))
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Recurring task completion $completionId cannot be reset because it already has a reward transaction",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `approveRecurringTaskCompletion rejects parents outside the family`() {
        val memberParent = parentRepository.save(parent("Jane Doe"))
        val outsiderParent = parentRepository.save(parent("John Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), memberParent, "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val occurrenceDate = LocalDate.now()
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = memberParent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_APPROVAL,
                    daysOfWeek = setOf(occurrenceDate.dayOfWeek),
                    startsOn = occurrenceDate.minusWeeks(3),
                    endsOn = occurrenceDate.plusWeeks(3),
                    maxCompletionsPerPeriod = 2,
                ),
            )
        val completion =
            recurringTaskCompletionRepository.save(
                recurringCompletion(task = task, child = child, occurrenceDate = occurrenceDate),
            )

        authenticatedGraphQlTester(requireNotNull(outsiderParent.id))
            .document(approveRecurringTaskCompletionDocument)
            .variable("input", mapOf("completionId" to requireNotNull(completion.id)))
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Resource not found",
                    classification = "NOT_FOUND",
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

    private fun family(
        name: String,
        recurringTaskCompletionGracePeriodDays: Int = 3,
    ) = FamilyEntity().apply {
        this.name = name
        currencyCode = "USD"
        currencyName = "US Dollar"
        currencySymbol = "$"
        currencyPosition = PREFIX
        currencyMinorUnit = 2
        currencyKind = ISO_CURRENCY
        this.recurringTaskCompletionGracePeriodDays = recurringTaskCompletionGracePeriodDays
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

    private fun taskCategory(
        familyId: Long,
        name: String,
    ) = TaskCategoryEntity().apply {
        this.familyId = familyId
        this.name = name
    }

    private fun recurringTask(
        familyId: Long,
        parent: ParentEntity,
        category: TaskCategoryEntity,
        rewardPayoutPolicy: RewardPayoutPolicy,
        daysOfWeek: Set<DayOfWeek>,
        startsOn: LocalDate,
        endsOn: LocalDate,
        maxCompletionsPerPeriod: Int,
    ) = RecurringTaskEntity().apply {
        this.familyId = familyId
        title = "Clean room"
        this.category = category
        rewardAmountMinor = 200
        this.rewardPayoutPolicy = rewardPayoutPolicy
        eligibilityMode = EligibilityMode.ALL_CHILDREN
        createdByParent = parent
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
        updatedByParent = parent
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
        status = RecurringTaskStatus.ACTIVE
        recurrenceKind = TaskRecurrenceKind.WEEKLY
        recurrenceInterval = 1
        recurrenceDays = daysOfWeek
        recurrenceStartsOn = startsOn
        recurrenceEndsOn = endsOn
        recurrenceMaxCompletionsPerPeriod = maxCompletionsPerPeriod
    }

    private fun recurringCompletion(
        task: RecurringTaskEntity,
        child: ChildEntity,
        occurrenceDate: LocalDate,
    ) = RecurringTaskCompletionEntity().apply {
        recurringTaskId = requireNotNull(task.id)
        this.child = child
        this.occurrenceDate = occurrenceDate
        status = TaskCompletionStatus.COMPLETED
        completedAt = Instant.parse("2026-03-01T18:00:00Z")
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

private val completeRecurringTaskDocument =
    $$"""
    mutation CompleteRecurringTask($input: CompleteRecurringTaskInput!) {
      completeRecurringTask(input: $input) {
        completion {
          id
          occurrenceDate
          status
        }
        rewardTransaction {
          id
          taskCompletion {
            __typename
            ... on RecurringTaskCompletion {
              id
            }
          }
        }
      }
    }
    """.trimIndent()

private val approveRecurringTaskCompletionDocument =
    $$"""
    mutation ApproveRecurringTaskCompletion($input: ApproveRecurringTaskCompletionInput!) {
      approveRecurringTaskCompletion(input: $input) {
        completion {
          id
          status
        }
        rewardTransaction {
          id
          taskCompletion {
            __typename
            ... on RecurringTaskCompletion {
              id
            }
          }
        }
      }
    }
    """.trimIndent()

private val resetRecurringTaskCompletionToAvailableDocument =
    $$"""
    mutation ResetRecurringTaskCompletionToAvailable($input: ResetRecurringTaskCompletionToAvailableInput!) {
      resetRecurringTaskCompletionToAvailable(input: $input) {
        completion {
          id
          status
        }
      }
    }
    """.trimIndent()
