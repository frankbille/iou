package dk.frankbille.iou.auth

import dk.frankbille.iou.test.GraphQlControllerIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

class ParentAuthIntegrationTest : GraphQlControllerIntegrationTest() {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    @Autowired
    private lateinit var parentAuthCredentialRepository: ParentAuthCredentialRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `register hashes the password and returned token can create a family`() {
        val password = "super-secret-password"

        val responseBody =
            authClient()
                .post()
                .uri("/auth/parents/register")
                .contentType(APPLICATION_JSON)
                .bodyValue(registerRequestJson(email = "  Jane@example.com ", password = password))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
                .orEmpty()

        val accessToken = extractJsonString(responseBody, "accessToken")
        val parentId = extractParentId(responseBody)

        val credential = parentAuthCredentialRepository.findByEmail("jane@example.com")
        requireNotNull(credential)

        assertThat(credential.passwordHash).isNotEqualTo(password)
        assertThat(passwordEncoder.matches(password, credential.passwordHash)).isTrue()

        authenticatedGraphQlTesterWithBearerToken(accessToken)
            .document(createFamilyDocument)
            .variable(
                "input",
                mapOf(
                    "name" to "Doe Family",
                    "currency" to
                        mapOf(
                            "code" to "USD",
                            "name" to "US Dollar",
                            "symbol" to "$",
                            "position" to "PREFIX",
                            "minorUnit" to 2,
                            "kind" to "ISO_CURRENCY",
                        ),
                    "defaultRewardAccountName" to "Main",
                    "defaultRewardAccountKind" to "BANK",
                    "recurringTaskCompletionGracePeriodDays" to 3,
                ),
            ).execute()
            .path("createFamily.family.name")
            .entity<String>()
            .isEqualTo("Doe Family")

        authenticatedGraphQlTesterWithBearerToken(accessToken)
            .document(viewerDocument)
            .execute()
            .path("$.data.viewer.person.id")
            .entity<Long>()
            .isEqualTo(parentId)
            .path("$.data.viewer.families.length()")
            .entity<Long>()
            .isEqualTo(1L)
            .path("$.data.viewer.families[0].name")
            .entity<String>()
            .isEqualTo("Doe Family")
    }

    @Test
    fun `login returns a token for an existing parent credential`() {
        authClient()
            .post()
            .uri("/auth/parents/register")
            .contentType(APPLICATION_JSON)
            .bodyValue(registerRequestJson(email = "jane@example.com", password = "super-secret-password"))
            .exchange()
            .expectStatus()
            .isOk

        val responseBody =
            authClient()
                .post()
                .uri("/auth/parents/login")
                .contentType(APPLICATION_JSON)
                .bodyValue(loginRequestJson(email = "JANE@example.com", password = "super-secret-password"))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
                .orEmpty()

        val accessToken = extractJsonString(responseBody, "accessToken")

        authenticatedGraphQlTesterWithBearerToken(accessToken)
            .document(viewerDocument)
            .execute()
            .path("$.data.viewer.person.__typename")
            .entity<String>()
            .isEqualTo("Parent")
            .path("$.data.viewer.person.name")
            .entity<String>()
            .isEqualTo("Jane Doe")
            .path("$.data.viewer.families.length()")
            .entity<Long>()
            .isEqualTo(0L)
    }

    @Test
    fun `register rejects duplicate emails`() {
        authClient()
            .post()
            .uri("/auth/parents/register")
            .contentType(APPLICATION_JSON)
            .bodyValue(registerRequestJson(email = "jane@example.com", password = "super-secret-password"))
            .exchange()
            .expectStatus()
            .isOk

        authClient()
            .post()
            .uri("/auth/parents/register")
            .contentType(APPLICATION_JSON)
            .bodyValue(registerRequestJson(email = " JANE@example.com ", password = "super-secret-password"))
            .exchange()
            .expectStatus()
            .isEqualTo(409)
    }

    @Test
    fun `login rejects invalid credentials`() {
        authClient()
            .post()
            .uri("/auth/parents/register")
            .contentType(APPLICATION_JSON)
            .bodyValue(registerRequestJson(email = "jane@example.com", password = "super-secret-password"))
            .exchange()
            .expectStatus()
            .isOk

        authClient()
            .post()
            .uri("/auth/parents/login")
            .contentType(APPLICATION_JSON)
            .bodyValue(loginRequestJson(email = "jane@example.com", password = "wrong-password"))
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    private fun authenticatedGraphQlTesterWithBearerToken(bearerToken: String) =
        HttpGraphQlTester
            .create(
                MockMvcWebTestClient
                    .bindToApplicationContext(applicationContext)
                    .apply(springSecurity())
                    .configureClient()
                    .baseUrl("/graphql")
                    .build(),
            ).mutate()
            .headers { headers ->
                headers.setBearerAuth(bearerToken)
            }.build()

    private fun authClient(): WebTestClient =
        MockMvcWebTestClient
            .bindToApplicationContext(applicationContext)
            .configureClient()
            .build()
}

private val createFamilyDocument =
    $$"""
    mutation CreateFamily($input: CreateFamilyInput!) {
      createFamily(input: $input) {
        family {
          name
        }
      }
    }
    """

private fun registerRequestJson(
    email: String,
    password: String,
    name: String = "Jane Doe",
): String = """{"name":"$name","email":"$email","password":"$password"}"""

private fun loginRequestJson(
    email: String,
    password: String,
): String = """{"email":"$email","password":"$password"}"""

private fun extractJsonString(
    body: String,
    field: String,
): String =
    Regex(""""$field"\s*:\s*"([^"]+)"""")
        .find(body)
        ?.groupValues
        ?.get(1)
        ?: error("Missing string field '$field' in response: $body")

private fun extractParentId(body: String): Long =
    Regex(""""parent"\s*:\s*\{\s*"id"\s*:\s*(\d+)""")
        .find(body)
        ?.groupValues
        ?.get(1)
        ?.toLong()
        ?: error("Missing parent id in response: $body")

private val viewerDocument =
    """
    query Viewer {
      viewer {
        person {
          __typename
          ... on Parent {
            id
            name
          }
        }
        families {
          id
          name
        }
      }
    }
    """
