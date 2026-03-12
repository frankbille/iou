package dk.frankbille.iou.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class AuthenticatedParentJwtAuthenticationConverter(
    private val familyAuthorizationService: FamilyAuthorizationService,
) : Converter<Jwt, AuthenticatedParentAuthenticationToken> {
    override fun convert(jwt: Jwt): AuthenticatedParentAuthenticationToken {
        val subject = jwt.subject?.takeIf { it.isNotBlank() } ?: throw BadCredentialsException("Missing subject claim")
        val globalId = GlobalId.parse(subject)

        if (globalId.app != "iou") {
            throw BadCredentialsException("Unsupported GlobalID app: ${globalId.app}")
        }

        if (globalId.modelName != "Parent") {
            throw BadCredentialsException("Unsupported GlobalID model: ${globalId.modelName}")
        }

        val familyAuthorities =
            familyAuthorizationService
                .getAccessibleFamilyIdsForParent(globalId.modelId)
                .map { SimpleGrantedAuthority("FAMILY_$it") }

        return AuthenticatedParentAuthenticationToken(
            jwt = jwt,
            authenticatedParent = AuthenticatedParentPrincipal(globalId = globalId),
            authorities = listOf(SimpleGrantedAuthority("ROLE_PARENT")) + familyAuthorities,
        )
    }
}
