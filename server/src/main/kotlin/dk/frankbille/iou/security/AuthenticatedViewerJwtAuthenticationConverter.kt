package dk.frankbille.iou.security

import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.CHILD_MODEL_NAME
import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.PARENT_MODEL_NAME
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class AuthenticatedViewerJwtAuthenticationConverter(
    private val familyAuthorizationService: FamilyAuthorizationService,
) : Converter<Jwt, AuthenticatedViewerAuthenticationToken> {
    override fun convert(jwt: Jwt): AuthenticatedViewerAuthenticationToken {
        val subject = jwt.subject?.takeIf { it.isNotBlank() } ?: throw BadCredentialsException("Missing subject claim")
        val globalId = GlobalId.parse(subject)

        if (globalId.app != "iou") {
            throw BadCredentialsException("Unsupported GlobalID app: ${globalId.app}")
        }

        val role =
            when (globalId.modelName) {
                PARENT_MODEL_NAME -> "ROLE_PARENT"
                CHILD_MODEL_NAME -> "ROLE_CHILD"
                else -> throw BadCredentialsException("Unsupported GlobalID model: ${globalId.modelName}")
            }

        val familyAuthorities =
            familyAuthorizationService
                .getAccessibleFamilyIdsForViewer(globalId)
                .map { SimpleGrantedAuthority("FAMILY_$it") }

        return AuthenticatedViewerAuthenticationToken(
            jwt = jwt,
            authenticatedViewer = AuthenticatedViewerPrincipal(globalId = globalId),
            authorities = listOf(SimpleGrantedAuthority(role)) + familyAuthorities,
        )
    }
}
