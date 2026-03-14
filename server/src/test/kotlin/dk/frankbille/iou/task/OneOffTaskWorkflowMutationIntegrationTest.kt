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
import java.time.Instant

class OneOffTaskWorkflowMutationIntegrationTest : GraphQlControllerIntegrationTest() {
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
    private lateinit var oneOffTaskRepository: OneOffTaskRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var rewardTransactionRepository: RewardTransactionRepository

    @Autowired
    private lateinit var taskCategoryRepository: TaskCategoryRepository

    @Test
    fun `completeOneOffTask pays immediately for ON_COMPLETION and resolves taskCompletion`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val rewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH))
        family.defaultRewardAccount = rewardAccount
        familyRepository.save(family)
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
                ),
            )

        authenticatedChildGraphQlTester(requireNotNull(child.id))
            .document(completeOneOffTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()
            .path("completeOneOffTask.task.status")
            .entity<String>()
            .isEqualTo("COMPLETED")
            .path("completeOneOffTask.rewardTransaction.toAccount.id")
            .entity<String>()
            .isEqualTo(requireNotNull(rewardAccount.id).toString())
            .path("completeOneOffTask.rewardTransaction.owner.id")
            .entity<String>()
            .isEqualTo(requireNotNull(parent.id).toString())
            .path("completeOneOffTask.rewardTransaction.taskCompletion.__typename")
            .entity<String>()
            .isEqualTo("OneOffTask")
            .path("completeOneOffTask.rewardTransaction.taskCompletion.id")
            .entity<String>()
            .isEqualTo(requireNotNull(task.id).toString())

        val savedTask = oneOffTaskRepository.findById(requireNotNull(task.id)).orElseThrow()
        assertThat(savedTask.status).isEqualTo(TaskCompletionStatus.COMPLETED)
        assertThat(requireNotNull(savedTask.completedChild).id).isEqualTo(child.id)
        assertThat(rewardTransactionRepository.findByOneOffTaskId(requireNotNull(task.id))).isNotNull
    }

    @Test
    fun `approveOneOffTask creates reward transaction for ON_APPROVAL`() {
        val creatorParent = parentRepository.save(parent("Jane Doe"))
        val approvingParent = parentRepository.save(parent("John Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), creatorParent, "Mom"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), approvingParent, "Dad"))
        val rewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH))
        family.defaultRewardAccount = rewardAccount
        familyRepository.save(family)
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = creatorParent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_APPROVAL,
                ),
            )

        authenticatedChildGraphQlTester(requireNotNull(child.id))
            .document(completeOneOffTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()

        authenticatedGraphQlTester(requireNotNull(approvingParent.id))
            .document(approveOneOffTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()
            .path("approveOneOffTask.task.status")
            .entity<String>()
            .isEqualTo("APPROVED")
            .path("approveOneOffTask.rewardTransaction.owner.id")
            .entity<String>()
            .isEqualTo(requireNotNull(approvingParent.id).toString())
            .path("approveOneOffTask.rewardTransaction.taskCompletion.__typename")
            .entity<String>()
            .isEqualTo("OneOffTask")

        val savedTask = oneOffTaskRepository.findById(requireNotNull(task.id)).orElseThrow()
        assertThat(savedTask.status).isEqualTo(TaskCompletionStatus.APPROVED)
        assertThat(savedTask.approvedByParent?.id).isEqualTo(approvingParent.id)
        assertThat(rewardTransactionRepository.findByOneOffTaskId(requireNotNull(task.id))).isNotNull
    }

    @Test
    fun `resetOneOffTaskToAvailable rejects tasks with reward transactions`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val rewardAccount = moneyAccountRepository.save(moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH))
        family.defaultRewardAccount = rewardAccount
        familyRepository.save(family)
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                    rewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
                ),
            )

        authenticatedChildGraphQlTester(requireNotNull(child.id))
            .document(completeOneOffTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()

        authenticatedGraphQlTester(requireNotNull(parent.id))
            .document(resetOneOffTaskToAvailableDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "One-off task ${requireNotNull(task.id)} cannot be reset because it already has a reward transaction",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `completeOneOffTask rejects authenticated parents`() {
        val memberParent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), memberParent, "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child("Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, "Daughter"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = memberParent,
                    category = category,
                ),
            )

        authenticatedGraphQlTester(requireNotNull(memberParent.id))
            .document(completeOneOffTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()
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

    @Test
    fun `completeOneOffTask rejects ineligible authenticated child`() {
        val parent = parentRepository.save(parent("Jane Doe"))
        val family = familyRepository.save(family("Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val eligibleChild = childRepository.save(child("Ava"))
        val ineligibleChild = childRepository.save(child("Noah"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), eligibleChild, "Daughter"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), ineligibleChild, "Son"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ).apply {
                    eligibilityMode = EligibilityMode.RESTRICTED
                    eligibleChildren = mutableSetOf(eligibleChild)
                },
            )

        authenticatedChildGraphQlTester(requireNotNull(ineligibleChild.id))
            .document(completeOneOffTaskDocument)
            .variable("input", mapOf("taskId" to requireNotNull(task.id)))
            .execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Child ${requireNotNull(ineligibleChild.id)} is not eligible to complete task ${requireNotNull(task.id)}",
                    classification = "BAD_REQUEST",
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

    private fun taskCategory(
        familyId: Long,
        name: String,
    ) = TaskCategoryEntity().apply {
        this.familyId = familyId
        this.name = name
    }

    private fun oneOffTask(
        familyId: Long,
        parent: ParentEntity,
        category: TaskCategoryEntity,
        rewardPayoutPolicy: RewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
    ) = OneOffTaskEntity().apply {
        this.familyId = familyId
        title = "Wash dishes"
        this.category = category
        rewardAmountMinor = 150
        this.rewardPayoutPolicy = rewardPayoutPolicy
        status = TaskCompletionStatus.AVAILABLE
        createdByParent = parent
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
        updatedByParent = parent
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
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

private val completeOneOffTaskDocument =
    $$"""
    mutation CompleteOneOffTask($input: CompleteOneOffTaskInput!) {
      completeOneOffTask(input: $input) {
        task {
          id
          status
        }
        rewardTransaction {
          id
          owner {
            id
          }
          toAccount {
            id
          }
          taskCompletion {
            __typename
            ... on OneOffTask {
              id
            }
          }
        }
      }
    }
    """.trimIndent()

private val approveOneOffTaskDocument =
    $$"""
    mutation ApproveOneOffTask($input: ApproveOneOffTaskInput!) {
      approveOneOffTask(input: $input) {
        task {
          id
          status
        }
        rewardTransaction {
          id
          owner {
            id
          }
          taskCompletion {
            __typename
            ... on OneOffTask {
              id
            }
          }
        }
      }
    }
    """.trimIndent()

private val resetOneOffTaskToAvailableDocument =
    $$"""
    mutation ResetOneOffTaskToAvailable($input: ResetOneOffTaskToAvailableInput!) {
      resetOneOffTaskToAvailable(input: $input) {
        task {
          id
          status
        }
      }
    }
    """.trimIndent()
