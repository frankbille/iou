package dk.frankbille.iou.security

import com.nimbusds.jose.JOSEObjectType.JWT
import com.nimbusds.jose.JWSAlgorithm.HS256
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.Date

object TestJwtFactory {
    private const val JWT_SECRET = "integration-test-jwt-secret-0123456789abcdef"

    fun createBearerToken(parentId: Long): String {
        val now = Instant.now()
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(parentGlobalId(parentId))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build()

        val signedJwt = SignedJWT(JWSHeader.Builder(HS256).type(JWT).build(), claims)
        signedJwt.sign(MACSigner(JWT_SECRET.toByteArray()))
        return signedJwt.serialize()
    }

    fun createAuthentication(
        parentId: Long,
        familyIds: List<Long> = emptyList(),
        includeParentRole: Boolean = true,
    ): AuthenticatedParentAuthenticationToken {
        val now = Instant.now()
        val jwt =
            Jwt
                .withTokenValue("test-token-$parentId")
                .header("alg", "HS256")
                .subject(parentGlobalId(parentId))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()

        val authorities =
            familyIds.map { SimpleGrantedAuthority("FAMILY_$it") }.toMutableList().apply {
                if (includeParentRole) {
                    add(0, SimpleGrantedAuthority("ROLE_PARENT"))
                }
            }

        return AuthenticatedParentAuthenticationToken(
            jwt = jwt,
            authenticatedParent = AuthenticatedParentPrincipal(globalId = GlobalId.parse(parentGlobalId(parentId))),
            authorities = authorities,
        )
    }

    private fun parentGlobalId(parentId: Long): String = "gid://iou/Parent/$parentId"
}
