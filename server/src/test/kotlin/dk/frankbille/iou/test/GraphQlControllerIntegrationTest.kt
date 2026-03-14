package dk.frankbille.iou.test

import dk.frankbille.iou.security.TestJwtFactory
import jakarta.persistence.CollectionTable
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.JoinTable
import jakarta.persistence.Table
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    webEnvironment = MOCK,
    properties = ["spring.liquibase.contexts=schema"],
)
@AutoConfigureHttpGraphQlTester
@Import(IntegrationTestConfiguration::class)
abstract class GraphQlControllerIntegrationTest {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeEach
    fun clearDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        tablesToTruncate().forEach { truncateTable(it) }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    protected fun tablesToTruncate(): List<String> =
        linkedSetOf<String>()
            .apply {
                entityManagerFactory.metamodel.entities.map { it.javaType }.forEach { entityClass ->
                    classHierarchy(entityClass).forEach { currentClass ->
                        currentClass
                            .getAnnotation(Table::class.java)
                            ?.name
                            ?.takeIf(String::isNotBlank)
                            ?.let(::add)
                        currentClass.declaredFields.forEach { field ->
                            field
                                .getAnnotation(JoinTable::class.java)
                                ?.name
                                ?.takeIf(String::isNotBlank)
                                ?.let(::add)
                            field
                                .getAnnotation(CollectionTable::class.java)
                                ?.name
                                ?.takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                        currentClass.declaredMethods.forEach { method ->
                            method
                                .getAnnotation(JoinTable::class.java)
                                ?.name
                                ?.takeIf(String::isNotBlank)
                                ?.let(::add)
                            method
                                .getAnnotation(CollectionTable::class.java)
                                ?.name
                                ?.takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                    }
                }
            }.toList()

    private fun classHierarchy(entityClass: Class<*>): List<Class<*>> =
        generateSequence(entityClass) { current ->
            current.superclass?.takeIf { it != Any::class.java }
        }.toList()

    protected fun truncateTable(tableName: String) {
        jdbcTemplate.execute("TRUNCATE TABLE $tableName")
    }

    protected fun graphQlTester() =
        HttpGraphQlTester.create(
            MockMvcWebTestClient
                .bindToApplicationContext(applicationContext)
                .configureClient()
                .baseUrl("/graphql")
                .build(),
        )

    protected fun authenticatedGraphQlTester(parentId: Long) =
        authenticatedGraphQlTesterWithBearerToken(TestJwtFactory.createParentBearerToken(parentId))

    protected fun authenticatedChildGraphQlTester(childId: Long) =
        authenticatedGraphQlTesterWithBearerToken(TestJwtFactory.createChildBearerToken(childId))

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
}
