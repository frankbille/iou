package dk.frankbille.iou.security

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.family.FamilyService
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.entity
import org.springframework.security.access.AccessDeniedException

class SecurityIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var familyService: FamilyService

    @Test
    fun `graphql endpoint requires authentication`() {
        graphQlTester()
            .document(viewerQuery)
            .execute()
            .errors()
            .expect { it.message == "Unauthorized" }
    }

    @Test
    fun `viewer is resolved from authenticated parent and families are membership scoped`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))

        val authorizedFamily = familyRepository.save(family(name = "Authorized family"))
        val otherFamily = familyRepository.save(family(name = "Other family"))

        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(authorizedFamily.id),
                parent = parent,
                relation = "Mom",
            ),
        )
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(otherFamily.id),
                parent = otherParent,
                relation = "Dad",
            ),
        )

        authenticatedGraphQlTester(parent.id!!)
            .document(viewerQuery)
            .execute()
            .path("$.data.viewer.person.id")
            .entity<Long>()
            .isEqualTo(parent.id!!)
            .path("$.data.viewer.person.name")
            .entity<String>()
            .isEqualTo("Jane Doe")
            .path("$.data.viewer.families.length()")
            .entity<Long>()
            .isEqualTo(1L)
            .path("$.data.viewer.families[0].id")
            .entity<Long>()
            .isEqualTo(authorizedFamily.id!!)
            .path("$.data.viewer.families[0].name")
            .entity<String>()
            .isEqualTo("Authorized family")
    }

    @Test
    @WithAuthenticatedParent(parentId = 1L, familyIds = [1L])
    fun `family access is denied when parent is not a member`() {
        val authorizedParent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))
        val authorizedFamily = familyRepository.save(family(name = "Authorized family"))
        val restrictedFamily = familyRepository.save(family(name = "Restricted family"))

        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(authorizedFamily.id),
                parent = authorizedParent,
                relation = "Mom",
            ),
        )
        familyParentRepository.save(
            familyParent(
                familyId = requireNotNull(restrictedFamily.id),
                parent = otherParent,
                relation = "Dad",
            ),
        )

        assertThatThrownBy { familyService.getFamily(requireNotNull(restrictedFamily.id)) }
            .isInstanceOf(AccessDeniedException::class.java)
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

@Language("GraphQL")
private val viewerQuery =
    """
    query ViewerQuery {
        viewer {
            person {
                id 
                ... on Parent { 
                    name 
                } 
            }
            families {
                id
                name
            }
        }
    }
    """.trimIndent()
