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

object TestJwtFactory {
    private const val jwtSecret = "integration-test-jwt-secret-0123456789abcdef"

    fun createBearerToken(parentId: Long): String {
        val now = Instant.now()
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(parentGlobalId(parentId))
                .issueTime(java.util.Date.from(now))
                .expirationTime(java.util.Date.from(now.plusSeconds(3600)))
                .build()

        val signedJwt = SignedJWT(JWSHeader.Builder(HS256).type(JWT).build(), claims)
        signedJwt.sign(MACSigner(jwtSecret.toByteArray()))
        return signedJwt.serialize()
    }

    fun createAuthentication(parentId: Long): AuthenticatedParentAuthenticationToken {
        val now = Instant.now()
        val jwt =
            Jwt
                .withTokenValue("test-token-$parentId")
                .header("alg", "HS256")
                .subject(parentGlobalId(parentId))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()

        return AuthenticatedParentAuthenticationToken(
            jwt = jwt,
            authenticatedParent = AuthenticatedParentPrincipal(globalId = GlobalId.parse(parentGlobalId(parentId))),
            authorities = listOf(SimpleGrantedAuthority("ROLE_PARENT")),
        )
    }

    private fun parentGlobalId(parentId: Long): String = "gid://iou/Parent/$parentId"
}
