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
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import dk.frankbille.iou.taskcategory.TaskCategoryRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.Instant
import java.time.LocalDate

class RecurringTaskMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var childRepository: ChildRepository

    @Autowired
    private lateinit var familyChildRepository: FamilyChildRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var recurringTaskCompletionRepository: RecurringTaskCompletionRepository

    @Autowired
    private lateinit var recurringTaskRepository: RecurringTaskRepository

    @Autowired
    private lateinit var taskCategoryRepository: TaskCategoryRepository

    @Test
    fun `createRecurringTask creates recurrence and audit fields`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child(name = "Ava"))
        familyChildRepository.save(familyChild(requireNotNull(family.id), child, relation = "Daughter"))

        executeCreateRecurringTask(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "title" to "Clean room",
                    "description" to "Thursday reset",
                    "categoryId" to requireNotNull(category.id),
                    "rewardAmountMinor" to 200,
                    "rewardPayoutPolicy" to "ON_APPROVAL",
                    "eligibleChildIds" to listOf(requireNotNull(child.id)),
                    "recurrence" to recurrenceInput(daysOfWeek = listOf("MONDAY", "THURSDAY"), interval = 2),
                ),
        ).path("createRecurringTask.task.title")
            .entity<String>()
            .isEqualTo("Clean room")
            .path("createRecurringTask.task.recurrence.daysOfWeek[0]")
            .entity<String>()
            .isEqualTo("MONDAY")

        val task = recurringTaskRepository.findAll().single()
        assertThat(task.title).isEqualTo("Clean room")
        assertThat(task.recurrenceKind).isEqualTo(TaskRecurrenceKind.WEEKLY)
        assertThat(task.recurrenceDays).containsExactlyInAnyOrder(MONDAY, THURSDAY)
        assertThat(task.createdByParent.id).isEqualTo(parent.id)
    }

    @Test
    fun `updateRecurringTask updates recurrence before completions exist`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )

        executeUpdateRecurringTask(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "taskId" to requireNotNull(task.id),
                    "title" to "Clean room better",
                    "description" to "Thursday reset",
                    "categoryId" to requireNotNull(category.id),
                    "rewardAmountMinor" to 250,
                    "rewardPayoutPolicy" to "ON_APPROVAL",
                    "eligibleChildIds" to null,
                    "recurrence" to recurrenceInput(daysOfWeek = listOf("THURSDAY"), interval = 1),
                ),
        ).path("updateRecurringTask.task.title")
            .entity<String>()
            .isEqualTo("Clean room better")

        val updatedTask = recurringTaskRepository.findById(requireNotNull(task.id)).orElseThrow()
        assertThat(updatedTask.title).isEqualTo("Clean room better")
        assertThat(updatedTask.recurrenceInterval).isEqualTo(1)
        assertThat(updatedTask.recurrenceDays).containsExactly(THURSDAY)
    }

    @Test
    fun `updateRecurringTask rejects recurrence changes after completions exist`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child(name = "Ava"))
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )
        recurringTaskCompletionRepository.save(
            recurringCompletion(task = task, child = child),
        )

        executeUpdateRecurringTask(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "taskId" to requireNotNull(task.id),
                    "title" to "Clean room",
                    "description" to null,
                    "categoryId" to requireNotNull(category.id),
                    "rewardAmountMinor" to 200,
                    "rewardPayoutPolicy" to "ON_COMPLETION",
                    "eligibleChildIds" to null,
                    "recurrence" to recurrenceInput(daysOfWeek = listOf("MONDAY"), interval = 1),
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot change recurrence for task ${requireNotNull(task.id)} after completions exist",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `archiveRecurringTask archives the task`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )

        executeArchiveRecurringTask(
            parentId = requireNotNull(parent.id),
            taskId = requireNotNull(task.id),
        ).path("archiveRecurringTask.task.recurringTaskStatus")
            .entity<String>()
            .isEqualTo("ARCHIVED")

        assertThat(recurringTaskRepository.findById(requireNotNull(task.id)).orElseThrow().status)
            .isEqualTo(RecurringTaskStatus.ARCHIVED)
    }

    @Test
    fun `deleteRecurringTask removes tasks without completions`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )

        executeDeleteRecurringTask(
            parentId = requireNotNull(parent.id),
            taskId = requireNotNull(task.id),
        ).path("deleteRecurringTask.deletedTaskId")
            .entity<String>()
            .isEqualTo(requireNotNull(task.id).toString())

        assertThat(recurringTaskRepository.findById(requireNotNull(task.id))).isEmpty()
    }

    @Test
    fun `deleteRecurringTask rejects tasks that already have completions`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val category = taskCategoryRepository.save(taskCategory(requireNotNull(family.id), "Chores"))
        val child = childRepository.save(child(name = "Ava"))
        val task =
            recurringTaskRepository.save(
                recurringTask(
                    familyId = requireNotNull(family.id),
                    parent = parent,
                    category = category,
                ),
            )
        recurringTaskCompletionRepository.save(recurringCompletion(task = task, child = child))

        executeDeleteRecurringTask(
            parentId = requireNotNull(parent.id),
            taskId = requireNotNull(task.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot delete task ${requireNotNull(task.id)} because it already has completions",
                    classification = "BAD_REQUEST",
                )
            }
    }

    private fun executeCreateRecurringTask(
        parentId: Long,
        input: Map<String, Any?>,
    ) = authenticatedGraphQlTester(parentId)
        .document(createRecurringTaskDocument)
        .variable("input", input)
        .execute()

    private fun executeUpdateRecurringTask(
        parentId: Long,
        input: Map<String, Any?>,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateRecurringTaskDocument)
        .variable("input", input)
        .execute()

    private fun executeArchiveRecurringTask(
        parentId: Long,
        taskId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(archiveRecurringTaskDocument)
        .variable("input", mapOf("taskId" to taskId))
        .execute()

    private fun executeDeleteRecurringTask(
        parentId: Long,
        taskId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(deleteRecurringTaskDocument)
        .variable("input", mapOf("taskId" to taskId))
        .execute()

    private fun recurrenceInput(
        daysOfWeek: List<String>,
        interval: Int,
    ) = mapOf(
        "kind" to "WEEKLY",
        "interval" to interval,
        "daysOfWeek" to daysOfWeek,
        "dayOfMonth" to null,
        "startsOn" to "2026-01-01",
        "endsOn" to "2026-03-31",
        "maxCompletionsPerPeriod" to 2,
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

    private fun recurringTask(
        familyId: Long,
        parent: ParentEntity,
        category: TaskCategoryEntity,
    ) = RecurringTaskEntity().apply {
        this.familyId = familyId
        title = "Clean room"
        this.category = category
        rewardAmountMinor = 200
        rewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION
        eligibilityMode = EligibilityMode.ALL_CHILDREN
        createdByParent = parent
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
        updatedByParent = parent
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
        status = RecurringTaskStatus.ACTIVE
        recurrenceKind = TaskRecurrenceKind.WEEKLY
        recurrenceInterval = 2
        recurrenceDays = setOf(MONDAY, THURSDAY)
        recurrenceStartsOn = LocalDate.parse("2026-01-01")
        recurrenceEndsOn = LocalDate.parse("2026-03-31")
        recurrenceMaxCompletionsPerPeriod = 2
    }

    private fun recurringCompletion(
        task: RecurringTaskEntity,
        child: ChildEntity,
    ) = RecurringTaskCompletionEntity().apply {
        recurringTaskId = requireNotNull(task.id)
        this.child = child
        occurrenceDate = LocalDate.parse("2026-01-05")
        status = TaskCompletionStatus.COMPLETED
        completedAt = Instant.parse("2026-01-05T18:00:00Z")
    }
}

private val createRecurringTaskDocument =
    $$"""
    mutation CreateRecurringTask($input: CreateRecurringTaskInput!) {
      createRecurringTask(input: $input) {
        task {
          id
          title
          recurrence {
            daysOfWeek
          }
        }
      }
    }
    """.trimIndent()

private val updateRecurringTaskDocument =
    $$"""
    mutation UpdateRecurringTask($input: UpdateRecurringTaskInput!) {
      updateRecurringTask(input: $input) {
        task {
          id
          title
        }
      }
    }
    """.trimIndent()

private val archiveRecurringTaskDocument =
    $$"""
    mutation ArchiveRecurringTask($input: ArchiveRecurringTaskInput!) {
      archiveRecurringTask(input: $input) {
        task {
          id
          recurringTaskStatus
        }
      }
    }
    """.trimIndent()

private val deleteRecurringTaskDocument =
    $$"""
    mutation DeleteRecurringTask($input: DeleteRecurringTaskInput!) {
      deleteRecurringTask(input: $input) {
        deletedTaskId
      }
    }
    """.trimIndent()
