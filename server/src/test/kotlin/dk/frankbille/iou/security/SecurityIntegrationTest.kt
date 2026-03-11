package dk.frankbille.iou.security

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.test.IntegrationTestConfiguration
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Import(IntegrationTestConfiguration::class)
class SecurityIntegrationTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var familyAuthorizationService: FamilyAuthorizationService

    @BeforeEach
    fun cleanDatabase() {
        SecurityContextHolder.clearContext()
        familyParentRepository.deleteAll()
        familyRepository.deleteAll()
        parentRepository.deleteAll()
    }

    @Test
    fun `graphql endpoint requires authentication`() {
        webTestClient
            .post()
            .uri("/graphql")
            .contentType(APPLICATION_JSON)
            .bodyValue(viewerQueryRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `viewer is resolved from authenticated parent and families are membership scoped`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))

        val authorizedFamily = familyRepository.save(family(name = "Authorized family"))
        val otherFamily = familyRepository.save(family(name = "Other family"))

        familyParentRepository.save(familyParent(familyId = requireNotNull(authorizedFamily.id), parent = parent, relation = "Mom"))
        familyParentRepository.save(familyParent(familyId = requireNotNull(otherFamily.id), parent = otherParent, relation = "Dad"))

        webTestClient
            .post()
            .uri("/graphql")
            .header("Authorization", "Bearer ${TestJwtFactory.createBearerToken(parentId = requireNotNull(parent.id))}")
            .contentType(APPLICATION_JSON)
            .bodyValue(viewerQueryRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.viewer.person.id")
            .isEqualTo(requireNotNull(parent.id).toString())
            .jsonPath("$.data.viewer.person.name")
            .isEqualTo("Jane Doe")
            .jsonPath("$.data.viewer.families.length()")
            .isEqualTo(1)
            .jsonPath("$.data.viewer.families[0].id")
            .isEqualTo(requireNotNull(authorizedFamily.id).toString())
            .jsonPath("$.data.viewer.families[0].name")
            .isEqualTo("Authorized family")
    }

    @Test
    fun `family access is denied when parent is not a member`() {
        val authorizedParent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))
        val restrictedFamily = familyRepository.save(family(name = "Restricted family"))

        familyParentRepository.save(familyParent(familyId = requireNotNull(restrictedFamily.id), parent = otherParent, relation = "Dad"))

        runAsAuthenticatedParent(requireNotNull(authorizedParent.id)) {
            assertThatThrownBy { familyAuthorizationService.requireAccess(requireNotNull(restrictedFamily.id)) }
                .isInstanceOf(AccessDeniedException::class.java)
        }
    }

    private fun runAsAuthenticatedParent(
        parentId: Long,
        block: () -> Unit,
    ) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = TestJwtFactory.createAuthentication(parentId)
        SecurityContextHolder.setContext(context)

        try {
            block()
        } finally {
            SecurityContextHolder.clearContext()
        }
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

    companion object {
        private val viewerQueryRequest =
            mapOf(
                "query" to "query ViewerQuery { viewer { person { id ... on Parent { name } } families { id name } } }",
            )
    }
}
