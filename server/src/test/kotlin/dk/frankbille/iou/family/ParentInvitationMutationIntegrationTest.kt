package dk.frankbille.iou.family

import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.invitation.ParentInvitationEntity
import dk.frankbille.iou.invitation.ParentInvitationRepository
import dk.frankbille.iou.invitation.ParentInvitationStatus.PENDING
import dk.frankbille.iou.invitation.ParentInvitationStatus.REVOKED
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.ResponseError
import org.springframework.graphql.test.tester.entity
import java.time.Instant

class ParentInvitationMutationIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var parentInvitationRepository: ParentInvitationRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Test
    fun `inviteParentToFamily creates a pending invitation`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))

        executeInviteParent(
            parentId = requireNotNull(parent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "email" to "Grandma@example.com",
                ),
        ).path("inviteParentToFamily.invitation.email")
            .entity<String>()
            .isEqualTo("grandma@example.com")
            .path("inviteParentToFamily.invitation.status")
            .entity<String>()
            .isEqualTo("PENDING")

        val invitation = parentInvitationRepository.findAll().single()
        assertThat(invitation.email).isEqualTo("grandma@example.com")
        assertThat(invitation.status).isEqualTo(PENDING)
        assertThat(invitation.invitedByParent.id).isEqualTo(parent.id)
        assertThat(invitation.invitationNonce).isNotBlank()
    }

    @Test
    fun `inviteParentToFamily rejects parents outside the family`() {
        val authorizedParent = parentRepository.save(parent(name = "Jane Doe"))
        val otherParent = parentRepository.save(parent(name = "John Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), otherParent, relation = "Dad"))

        executeInviteParent(
            parentId = requireNotNull(authorizedParent.id),
            input =
                mapOf(
                    "familyId" to requireNotNull(family.id),
                    "email" to "grandma@example.com",
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
    }

    @Test
    fun `revokeParentInvitation revokes a pending invitation`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val invitation =
            parentInvitationRepository.save(
                invitation(
                    familyId = requireNotNull(family.id),
                    invitedByParent = parent,
                    email = "grandma@example.com",
                    status = PENDING,
                ),
            )

        executeRevokeInvitation(
            parentId = requireNotNull(parent.id),
            invitationId = requireNotNull(invitation.id),
        ).path("revokeParentInvitation.invitation.status")
            .entity<String>()
            .isEqualTo("REVOKED")

        assertThat(parentInvitationRepository.findById(requireNotNull(invitation.id)).orElseThrow().status)
            .isEqualTo(REVOKED)
    }

    @Test
    fun `revokeParentInvitation rejects non pending invitations`() {
        val parent = parentRepository.save(parent(name = "Jane Doe"))
        val family = familyRepository.save(family(name = "Family One"))
        familyParentRepository.save(familyParent(requireNotNull(family.id), parent, relation = "Mom"))
        val invitation =
            parentInvitationRepository.save(
                invitation(
                    familyId = requireNotNull(family.id),
                    invitedByParent = parent,
                    email = "grandma@example.com",
                    status = REVOKED,
                ),
            )

        executeRevokeInvitation(
            parentId = requireNotNull(parent.id),
            invitationId = requireNotNull(invitation.id),
        ).errors()
            .satisfy { errors ->
                assertThat(errors).hasSize(1)
                assertGraphQlError(
                    error = errors.single(),
                    message = "Only pending invitations can be revoked",
                    classification = "BAD_REQUEST",
                )
            }
    }

    private fun executeInviteParent(
        parentId: Long,
        input: Map<String, Any>,
    ) = authenticatedGraphQlTester(parentId)
        .document(inviteParentDocument)
        .variable("input", input)
        .execute()

    private fun executeRevokeInvitation(
        parentId: Long,
        invitationId: Long,
    ) = authenticatedGraphQlTester(parentId)
        .document(revokeParentDocument)
        .variable("input", mapOf("invitationId" to invitationId))
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

    private fun invitation(
        familyId: Long,
        invitedByParent: ParentEntity,
        email: String,
        status: dk.frankbille.iou.invitation.ParentInvitationStatus,
    ) = ParentInvitationEntity().apply {
        this.familyId = familyId
        this.invitedByParent = invitedByParent
        this.email = email
        this.status = status
        this.createdAt = Instant.parse("2026-01-01T00:00:00Z")
        this.invitationNonce = "nonce-${email.substringBefore('@')}-${status.name.lowercase()}"
    }
}

private val inviteParentDocument =
    $$"""
    mutation InviteParentToFamily($input: InviteParentToFamilyInput!) {
      inviteParentToFamily(input: $input) {
        invitation {
          id
          email
          status
        }
      }
    }
    """.trimIndent()

private val revokeParentDocument =
    $$"""
    mutation RevokeParentInvitation($input: RevokeParentInvitationInput!) {
      revokeParentInvitation(input: $input) {
        invitation {
          id
          status
        }
      }
    }
    """.trimIndent()
