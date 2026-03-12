package dk.frankbille.iou.security

import org.springframework.security.core.Authentication

data class FamilyScopeExpressionRoot(
    val authentication: Authentication?,
) {
    val principal: Any?
        get() = authentication?.principal
}
