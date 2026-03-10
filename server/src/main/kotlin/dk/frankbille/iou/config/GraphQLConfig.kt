package dk.frankbille.iou.config

import graphql.scalars.datetime.DateScalar
import graphql.scalars.datetime.DateTimeScalar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class GraphQLConfig {
    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer {
            it.scalar(DateScalar.INSTANCE)
            it.scalar(DateTimeScalar.INSTANCE)
        }
}
