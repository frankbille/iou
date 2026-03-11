package dk.frankbille.iou.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

class AuthenticatedParentAuthenticationToken(
    private val jwt: Jwt,
    private val authenticatedParent: AuthenticatedParentPrincipal,
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): String = jwt.tokenValue

    override fun getPrincipal(): AuthenticatedParentPrincipal = authenticatedParent
}
