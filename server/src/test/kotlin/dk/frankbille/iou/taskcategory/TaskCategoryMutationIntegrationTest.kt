package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.task.OneOffTaskEntity
import dk.frankbille.iou.task.OneOffTaskRepository
import dk.frankbille.iou.task.RewardPayoutPolicy.ON_COMPLETION
import dk.frankbille.iou.task.TaskCompletionStatus.AVAILABLE
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity

class TaskCategoryMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var taskCategoryRepository: TaskCategoryRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var oneOffTaskRepository: OneOffTaskRepository

    @Test
    fun `createTaskCategory creates a category for an authorized family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(family.id),
                parent = parent,
                relation = "Mom",
            ),
        )

        executeCreateTaskCategory(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Chores",
                ),
        ).path("createTaskCategory.taskCategory.family.id")
            .entity<String>()
            .isEqualTo(requireNotNull(family.id).toString())
            .path("createTaskCategory.taskCategory.name")
            .entity<String>()
            .isEqualTo("Chores")

        assertThat(
            taskCategoryRepository
                .findAllByFamilyIdOrderByNameAsc(requireNotNull(family.id))
                .map(TaskCategoryEntity::name),
        ).containsExactly("Chores")
    }

    @Test
    fun `createTaskCategory rejects a parent outside the family`() {
        val authorizedParent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(family.id),
                parent = otherParent,
                relation = "Dad",
            ),
        )

        executeCreateTaskCategory(
            parentId = requireNotNull(authorizedParent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Chores",
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

        assertThat(taskCategoryRepository.findAll()).isEmpty()
    }

    @Test
    fun `createTaskCategory rejects blank names`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(family.id),
                parent = parent,
                relation = "Mom",
            ),
        )

        executeCreateTaskCategory(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "   ",
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Task category name must not be blank",
                    classification = "BAD_REQUEST",
                )
            }

        assertThat(taskCategoryRepository.findAll()).isEmpty()
    }

    @Test
    fun `createTaskCategory rejects duplicate names inside a family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(family.id),
                parent = parent,
                relation = "Mom",
            ),
        )
        taskCategoryRepository.save(
            TaskCategoryEntity().apply {
                familyId = requireNotNull(family.id)
                name = "Chores"
            },
        )

        executeCreateTaskCategory(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "name" to "Chores",
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Task category 'Chores' already exists in family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }

        assertThat(
            taskCategoryRepository
                .findAllByFamilyIdOrderByNameAsc(requireNotNull(family.id))
                .map(TaskCategoryEntity::name),
        ).containsExactly("Chores")
    }

    @Test
    fun `updateTaskCategory renames a category inside the same family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val taskCategory =
            taskCategoryRepository.save(
                TaskCategoryEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = "Chores"
                },
            )

        executeUpdateTaskCategory(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "taskCategoryId" to requireNotNull(taskCategory.id),
                    "name" to "Homework",
                ),
        ).path("updateTaskCategory.taskCategory.name")
            .entity<String>()
            .isEqualTo("Homework")

        assertThat(taskCategoryRepository.findById(requireNotNull(taskCategory.id)).orElseThrow().name)
            .isEqualTo("Homework")
    }

    @Test
    fun `updateTaskCategory rejects duplicate names inside the family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val taskCategory =
            taskCategoryRepository.save(
                TaskCategoryEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = "Chores"
                },
            )
        taskCategoryRepository.save(
            TaskCategoryEntity().apply {
                familyId = requireNotNull(family.id)
                name = "Homework"
            },
        )

        executeUpdateTaskCategory(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "taskCategoryId" to requireNotNull(taskCategory.id),
                    "name" to "Homework",
                ),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Task category 'Homework' already exists in family ${requireNotNull(family.id)}",
                    classification = "BAD_REQUEST",
                )
            }
    }

    @Test
    fun `deleteTaskCategory removes an unreferenced category`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val taskCategory =
            taskCategoryRepository.save(
                TaskCategoryEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = "Chores"
                },
            )

        executeDeleteTaskCategory(
            parentId = requireNotNull(parent.id),
            taskCategoryId = requireNotNull(taskCategory.id),
        ).path("deleteTaskCategory.deletedTaskCategoryId")
            .entity<String>()
            .isEqualTo(requireNotNull(taskCategory.id).toString())

        assertThat(taskCategoryRepository.findById(requireNotNull(taskCategory.id))).isEmpty()
    }

    @Test
    fun `deleteTaskCategory rejects categories referenced by tasks`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val taskCategory =
            taskCategoryRepository.save(
                TaskCategoryEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = "Chores"
                },
            )
        oneOffTaskRepository.save(
            OneOffTaskEntity().apply {
                familyId = requireNotNull(family.id)
                title = "Wash dishes"
                category = taskCategory
                rewardAmountMinor = 100
                rewardPayoutPolicy = ON_COMPLETION
                status = AVAILABLE
                createdByParent = parent
                createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z")
                updatedByParent = parent
                updatedAt = java.time.Instant.parse("2026-01-01T00:00:00Z")
            },
        )

        executeDeleteTaskCategory(
            parentId = requireNotNull(parent.id),
            taskCategoryId = requireNotNull(taskCategory.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Cannot delete task category ${requireNotNull(taskCategory.id)} because tasks still reference it",
                    classification = "BAD_REQUEST",
                )
            }
    }

    private fun executeCreateTaskCategory(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(createTaskCategoryDocument)
        .variable("input", input)
        .execute()

    private fun executeUpdateTaskCategory(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(updateTaskCategoryDocument)
        .variable("input", input)
        .execute()

    private fun executeDeleteTaskCategory(
        parentId: Long,
        taskCategoryId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(deleteTaskCategoryDocument)
        .variable("input", mapOf("taskCategoryId" to taskCategoryId))
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
}

private val createTaskCategoryDocument =
    $$"""
    mutation CreateTaskCategory($input: CreateTaskCategoryInput!) {
      createTaskCategory(input: $input) {
        taskCategory {
          id
          name
          family {
            id
          }
        }
      }
    }
    """.trimIndent()

private val updateTaskCategoryDocument =
    $$"""
    mutation UpdateTaskCategory($input: UpdateTaskCategoryInput!) {
      updateTaskCategory(input: $input) {
        taskCategory {
          id
          name
        }
      }
    }
    """.trimIndent()

private val deleteTaskCategoryDocument =
    $$"""
    mutation DeleteTaskCategory($input: DeleteTaskCategoryInput!) {
      deleteTaskCategory(input: $input) {
        deletedTaskCategoryId
      }
    }
    """.trimIndent()
