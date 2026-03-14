package dk.frankbille.iou.security

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithAuthenticatedChildSecurityContextFactory::class)
annotation class WithAuthenticatedChild(
    val childId: Long = 1L,
    val familyIds: LongArray = [],
)

class WithAuthenticatedChildSecurityContextFactory : WithSecurityContextFactory<WithAuthenticatedChild> {
    override fun createSecurityContext(annotation: WithAuthenticatedChild): SecurityContext =
        SecurityContextHolder.createEmptyContext().apply {
            authentication =
                TestJwtFactory.createChildAuthentication(
                    childId = annotation.childId,
                    familyIds = annotation.familyIds.toList(),
                )
        }
}
