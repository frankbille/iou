package dk.frankbille.iou.config

import graphql.GraphQLContext
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.CoercedVariables
import graphql.language.Value
import graphql.scalars.datetime.DateScalar
import graphql.scalars.datetime.DateTimeScalar
import graphql.schema.Coercing
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLScalarType
import jakarta.validation.ConstraintViolationException
import kotlinx.datetime.toJavaLocalDate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.execution.ErrorType.BAD_REQUEST
import org.springframework.graphql.execution.ErrorType.FORBIDDEN
import org.springframework.graphql.execution.ErrorType.NOT_FOUND
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.security.access.AccessDeniedException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.time.toJavaInstant
import kotlin.time.Instant as SharedInstant
import kotlinx.datetime.LocalDate as SharedLocalDate

@Configuration
class GraphQLConfig {
    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer {
            it.scalar(graphQlDateScalar())
            it.scalar(graphQlDateTimeScalar())
        }

    @Bean
    fun dataFetcherExceptionResolver() =
        object : DataFetcherExceptionResolverAdapter() {
            override fun resolveToSingleError(
                ex: Throwable,
                env: DataFetchingEnvironment,
            ): GraphQLError? =
                when (ex) {
                    is ConstraintViolationException -> graphQlError(BAD_REQUEST, ex.constraintViolations.first().message, env)
                    is IllegalArgumentException -> graphQlError(BAD_REQUEST, ex.message ?: "Bad request", env)
                    is AccessDeniedException -> graphQlError(FORBIDDEN, ex.message ?: "Forbidden", env)
                    is NoSuchElementException -> graphQlError(NOT_FOUND, ex.message ?: "Resource not found", env)
                    else -> null
                }
        }

    private fun graphQlError(
        errorType: ErrorType,
        message: String,
        env: DataFetchingEnvironment,
    ): GraphQLError =
        GraphqlErrorBuilder
            .newError(env)
            .errorType(errorType)
            .message(message)
            .build()

    private fun graphQlDateScalar(): GraphQLScalarType =
        with(DateScalar.INSTANCE) {
            GraphQLScalarType
                .newScalar(this)
                .coercing(
                    delegatingCoercing(coercing) { value ->
                        when (value) {
                            is SharedLocalDate -> value.toJavaLocalDate()
                            else -> value
                        }
                    },
                ).build()
        }

    private fun graphQlDateTimeScalar(): GraphQLScalarType =
        with(DateTimeScalar.INSTANCE) {
            GraphQLScalarType
                .newScalar(this)
                .coercing(
                    delegatingCoercing(coercing) { value ->
                        when (value) {
                            is SharedInstant -> OffsetDateTime.ofInstant(value.toJavaInstant(), ZoneOffset.UTC)
                            else -> value
                        }
                    },
                ).build()
        }

    @Suppress("UNCHECKED_CAST")
    private fun delegatingCoercing(
        delegate: Coercing<*, *>,
        normalizeOutput: (Any) -> Any,
    ): Coercing<Any, Any> {
        val typedDelegate = delegate as Coercing<Any, Any>

        return object : Coercing<Any, Any> {
            override fun serialize(
                dataFetcherResult: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Any = requireNotNull(typedDelegate.serialize(normalizeOutput(dataFetcherResult), graphQLContext, locale))

            override fun parseValue(
                input: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Any = requireNotNull(typedDelegate.parseValue(input, graphQLContext, locale))

            override fun parseLiteral(
                input: Value<*>,
                variables: CoercedVariables,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Any = requireNotNull(typedDelegate.parseLiteral(input, variables, graphQLContext, locale))

            override fun valueToLiteral(
                input: Any,
                graphQLContext: GraphQLContext,
                locale: Locale,
            ): Value<*> = typedDelegate.valueToLiteral(normalizeOutput(input), graphQLContext, locale)
        }
    }
}
