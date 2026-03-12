package dk.frankbille.iou.config

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.scalars.datetime.DateScalar
import graphql.scalars.datetime.DateTimeScalar
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolationException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.execution.ErrorType.BAD_REQUEST
import org.springframework.graphql.execution.ErrorType.FORBIDDEN
import org.springframework.graphql.execution.ErrorType.NOT_FOUND
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.security.access.AccessDeniedException

@Configuration
class GraphQLConfig {
    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer {
            it.scalar(DateScalar.INSTANCE)
            it.scalar(DateTimeScalar.INSTANCE)
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
}
