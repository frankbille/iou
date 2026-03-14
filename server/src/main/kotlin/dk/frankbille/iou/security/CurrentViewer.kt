package dk.frankbille.iou.security

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentViewer {
    fun parentId(): Long = authenticatedViewer().parentId

    fun childId(): Long = authenticatedViewer().childId

    fun isParent(): Boolean = authenticatedViewer().isParent

    fun isChild(): Boolean = authenticatedViewer().isChild

    fun authenticatedViewer(): AuthenticatedViewerPrincipal =
        SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedViewerPrincipal
            ?: throw AuthenticationCredentialsNotFoundException("No authenticated viewer is available")
}
