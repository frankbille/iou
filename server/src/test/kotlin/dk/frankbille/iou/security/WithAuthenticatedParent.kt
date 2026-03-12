package dk.frankbille.iou.security

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithAuthenticatedParentSecurityContextFactory::class)
annotation class WithAuthenticatedParent(
    val parentId: Long = 1L,
    val familyIds: LongArray = [],
    val includeParentRole: Boolean = true,
)

class WithAuthenticatedParentSecurityContextFactory : WithSecurityContextFactory<WithAuthenticatedParent> {
    override fun createSecurityContext(annotation: WithAuthenticatedParent): SecurityContext =
        SecurityContextHolder.createEmptyContext().apply {
            authentication =
                TestJwtFactory.createAuthentication(
                    parentId = annotation.parentId,
                    familyIds = annotation.familyIds.toList(),
                    includeParentRole = annotation.includeParentRole,
                )
        }
}
