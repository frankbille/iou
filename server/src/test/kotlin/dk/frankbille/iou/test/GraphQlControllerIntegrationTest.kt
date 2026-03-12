package dk.frankbille.iou.test

import dk.frankbille.iou.security.TestJwtFactory
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    webEnvironment = MOCK,
    properties = ["spring.liquibase.contexts=schema"],
)
@Import(IntegrationTestConfiguration::class)
abstract class GraphQlControllerIntegrationTest {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    protected lateinit var graphQlTester: HttpGraphQlTester

    @BeforeEach
    fun setUpGraphQlTester() {
        graphQlTester =
            HttpGraphQlTester.create(
                MockMvcWebTestClient
                    .bindToApplicationContext(applicationContext)
                    .apply(springSecurity())
                    .configureClient()
                    .baseUrl("/graphql")
                    .build(),
            )
    }

    protected fun authenticatedGraphQlTester(parentId: Long): HttpGraphQlTester =
        graphQlTester
            .mutate()
            .headers { headers ->
                headers.setBearerAuth(TestJwtFactory.createBearerToken(parentId))
            }.build()
}
