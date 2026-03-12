package dk.frankbille.iou.taskcategory

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun setUp() {
        taskCategoryRepository.deleteAll()
        familyParentRepository.deleteAll()
        familyRepository.deleteAll()
        parentRepository.deleteAll()
    }

    @Test
    fun `createTaskCategory creates a category for an authorized family`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(family.id),
                parent = parent,
                relation = "Mom"
            )
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
            taskCategoryRepository.findAllByFamilyIdOrderByNameAsc(requireNotNull(family.id))
                .map(TaskCategoryEntity::name)
        )
            .containsExactly("Chores")
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
                relation = "Dad"
            )
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
                relation = "Mom"
            )
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
                relation = "Mom"
            )
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
            taskCategoryRepository.findAllByFamilyIdOrderByNameAsc(requireNotNull(family.id))
                .map(TaskCategoryEntity::name)
        )
            .containsExactly("Chores")
    }

    private fun executeCreateTaskCategory(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(createTaskCategoryDocument)
        .variable("input", input)
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
