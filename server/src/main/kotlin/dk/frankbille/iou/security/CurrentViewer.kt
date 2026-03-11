package dk.frankbille.iou.security

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentViewer {
    fun parentId(): Long = authenticatedParent().parentId

    fun authenticatedParent(): AuthenticatedParentPrincipal =
        SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedParentPrincipal
            ?: throw AuthenticationCredentialsNotFoundException("No authenticated parent is available")
}
