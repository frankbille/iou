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
import dk.frankbille.iou.transaction.RewardTransactionEntity
import dk.frankbille.iou.transaction.RewardTransactionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity
import java.time.Instant

class OneOffTaskMutationIntegrationTest : GraphQlControllerIntegrationTest() {
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
    fun `createOneOffTask creates task configuration with category eligible children and audit`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val eligibleChild = childRepository.save(child(name = "Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), eligibleChild, relation = "Daughter"))

        executeCreateOneOffTask(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "title" to "Wash dishes",
                    "description" to "Kitchen sink",
                    "categoryId" to requireNotNull(category.id),
                    "rewardAmountMinor" to 150,
                    "rewardPayoutPolicy" to "ON_COMPLETION",
                    "eligibleChildIds" to listOf(requireNotNull(eligibleChild.id)),
                ),
        ).path("createOneOffTask.task.title")
            .entity<String>()
            .isEqualTo("Wash dishes")
            .path("createOneOffTask.task.category.id")
            .entity<String>()
            .isEqualTo(requireNotNull(category.id).toString())
            .path("createOneOffTask.task.eligibleChildren[0].id")
            .entity<String>()
            .isEqualTo(requireNotNull(eligibleChild.id).toString())
            .path("createOneOffTask.task.createdBy.id")
            .entity<String>()
            .isEqualTo(requireNotNull(parent.id).toString())

        val task = oneOffTaskRepository.findAll().single()
        assertThat(task.title).isEqualTo("Wash dishes")
        assertThat(requireNotNull(task.category).id).isEqualTo(category.id)
        assertThat(task.eligibilityMode).isEqualTo(EligibilityMode.RESTRICTED)
    }

    @Test
    fun `createOneOffTask rejects eligible children outside the family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val outsiderChild = childRepository.save(child(name = "Liam"))

        executeCreateOneOffTask(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "title" to "Wash dishes",
                    "description" to null,
                    "categoryId" to requireNotNull(category.id),
                    "rewardAmountMinor" to 150,
                    "rewardPayoutPolicy" to "ON_COMPLETION",
                    "eligibleChildIds" to listOf(requireNotNull(outsiderChild.id)),
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Eligible children [${requireNotNull(outsiderChild.id)}] do not belong to family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `updateOneOffTask updates task configuration`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val originalCategory = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val updatedCategory = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Homework"))
        val child = childRepository.save(child(name = "Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, relation = "Daughter"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = originalCategory,
                ),
            )

        executeUpdateOneOffTask(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "taskId" to requireNotNull(task.id),
                    "title" to "Finish math",
                    "description" to "Page 12",
                    "categoryId" to requireNotNull(updatedCategory.id),
                    "rewardAmountMinor" to 250,
                    "rewardPayoutPolicy" to "ON_APPROVAL",
                    "eligibleChildIds" to listOf(requireNotNull(child.id)),
                ),
        ).path("updateOneOffTask.task.title")
            .entity<String>()
            .isEqualTo("Finish math")
            .path("updateOneOffTask.task.reward.amountMinor")
            .entity<Int>()
            .isEqualTo(250)

        val updatedTask = oneOffTaskRepository.findById(requireNotNull(task.id)).orElseThrow()
        assertThat(updatedTask.title).isEqualTo("Finish math")
        assertThat(requireNotNull(updatedTask.category).id).isEqualTo(updatedCategory.id)
        assertThat(updatedTask.rewardAmountMinor).isEqualTo(250)
        assertThat(updatedTask.rewardPayoutPolicy).isEqualTo(RewardPayoutPolicy.ON_APPROVAL)
    }

    @Test
    fun `deleteOneOffTask removes tasks without reward transactions`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )

        executeDeleteOneOffTask(
            parentId = requireNotNull(parent.id),
            taskId = requireNotNull(task.id),
        ).path("deleteOneOffTask.deletedTaskId")
            .entity<String>()
            .isEqualTo(requireNotNull(task.id).toString())

        assertThat(oneOffTaskRepository.findById(requireNotNull(task.id))).isEmpty()
    }

    @Test
    fun `deleteOneOffTask rejects tasks that already have reward transactions`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child(name = "Ava"))
        val task =
            oneOffTaskRepository.save(
                oneOffTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )
        val account =
            moneyAccountRepository.save(
                moneyAccount(requireNotNull(family.id), "Allowance Wallet", MoneyAccountKind.CASH),
            )
        rewardTransactionRepository.save(
            RewardTransactionEntity().apply {
                this.familyId = requireNotNull(family.id)
                timestamp = Instant.parse("2026-01-01T00:00:00Z")
                amountMinor = 150
                ownerParent = parent
                this.child = child
                accountOne = account
                oneOffTask = task
            },
        )

        executeDeleteOneOffTask(
            parentId = requireNotNull(parent.id),
            taskId = requireNotNull(task.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot delete task ${requireNotNull(task.id)} because it already has a reward transaction",
                    classification = "BAD_REQUEST",
                )
            }
    }

    private fun executeCreateOneOffTask(
        parentId: Long,
        input: Map<String, Any?>,
    ) = authenticatedGraphQlTester(parentId)
        .document(createOneOffTaskDocument)
        .variable("input", input)
        .execute()

    private fun executeUpdateOneOffTask(
        parentId: Long,
        input: Map<String, Any?>,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateOneOffTaskDocument)
        .variable("input", input)
        .execute()

    private fun executeDeleteOneOffTask(
        parentId: Long,
        taskId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(deleteOneOffTaskDocument)
        .variable("input", mapOf("taskId" to taskId))
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
    ) = OneOffTaskEntity().apply {
        this.familyId = familyId
        title = "Wash dishes"
        this.category = category
        rewardAmountMinor = 150
        rewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION
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

private val createOneOffTaskDocument =
    $$"""
    mutation CreateOneOffTask($input: CreateOneOffTaskInput!) {
      createOneOffTask(input: $input) {
        task {
          id
          title
          category {
            id
          }
          eligibleChildren {
            id
          }
          createdBy {
            id
          }
        }
      }
    }
    """.trimIndent()

private val updateOneOffTaskDocument =
    $$"""
    mutation UpdateOneOffTask($input: UpdateOneOffTaskInput!) {
      updateOneOffTask(input: $input) {
        task {
          id
          title
          reward {
            amountMinor
          }
        }
      }
    }
    """.trimIndent()

private val deleteOneOffTaskDocument =
    $$"""
    mutation DeleteOneOffTask($input: DeleteOneOffTaskInput!) {
      deleteOneOffTask(input: $input) {
        deletedTaskId
      }
    }
    """.trimIndent()
