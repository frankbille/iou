package dk.frankbille.iou.security

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import dk.frankbille.iou.taskcategory.TaskCategoryRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

@Import(FamilyScopeCheckIntegrationTest.TestConfig::class)
class FamilyScopeCheckIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var taskCategoryRepository: TaskCategoryRepository

    @Autowired
    private lateinit var testService: FamilyScopeCheckTestService

    @Test
    @WithAuthenticatedParent(parentId = 1L, familyIds = [1L])
    fun `direct family scope allows access within authenticated family authorities`() {
        val family = familyRepository.save(family(name = "Authorized family"))

        assertThat(testService.directFamilyScope(requireNotNull(family.id))).isEqualTo("ok")
    }

    @Test
    @WithAuthenticatedParent(parentId = 123L)
    fun `direct family scope returns not found when family authority is missing`() {
        val family = familyRepository.save(family(name = "Restricted family"))

        assertThatThrownBy { testService.directFamilyScope(requireNotNull(family.id)) }
            .isInstanceOf(NotFoundInFamilyException::class.java)
            .hasMessage("Resource not found")
    }

    @Test
    @WithAuthenticatedParent(parentId = 1L, familyIds = [1L])
    fun `resource family scope allows access when resolved family matches authority`() {
        val family = familyRepository.save(family(name = "Authorized family"))
        val taskCategory =
            taskCategoryRepository.save(
                TaskCategoryEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = "Chores"
                },
            )

        assertThat(testService.taskCategoryFamilyScope(requireNotNull(taskCategory.id))).isEqualTo("ok")
    }

    @Test
    @WithAuthenticatedParent(parentId = 1L)
    fun `resource family scope returns not found when resolved family is outside authorities`() {
        val family = familyRepository.save(family(name = "Restricted family"))
        val taskCategory =
            taskCategoryRepository.save(
                TaskCategoryEntity().apply {
                    familyId = requireNotNull(family.id)
                    name = "Chores"
                },
            )

        assertThatThrownBy { testService.taskCategoryFamilyScope(requireNotNull(taskCategory.id)) }
            .isInstanceOf(NotFoundInFamilyException::class.java)
            .hasMessage("Resource not found")
    }

    @Test
    @WithAuthenticatedParent(parentId = 123L)
    fun `resource family scope returns not found when target resource does not exist`() {
        assertThatThrownBy { testService.taskCategoryFamilyScope(999_999L) }
            .isInstanceOf(NotFoundInFamilyException::class.java)
            .hasMessage("Resource not found")
    }

    @Test
    @WithAuthenticatedChild(childId = 1L, familyIds = [1L])
    fun `family scope check composes with spring security role checks`() {
        val family = familyRepository.save(family(name = "Authorized family"))

        assertThatThrownBy { testService.parentProtectedFamilyScope(requireNotNull(family.id)) }
            .isInstanceOf(AccessDeniedException::class.java)
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

    @TestConfiguration
    class TestConfig {
        @Bean
        fun familyScopeCheckTestService() = FamilyScopeCheckTestService()
    }
}

open class FamilyScopeCheckTestService {
    @FamilyScopeCheck("#familyId")
    open fun directFamilyScope(familyId: Long): String = "ok"

    @FamilyScopeCheck("@familyScopeResolver.taskCategoryFamilyId(#taskCategoryId)")
    open fun taskCategoryFamilyScope(taskCategoryId: Long): String = "ok"

    @IsParent
    @FamilyScopeCheck("#familyId")
    open fun parentProtectedFamilyScope(familyId: Long): String = "ok"
}
