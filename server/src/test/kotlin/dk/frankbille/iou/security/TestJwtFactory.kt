package dk.frankbille.iou.security

import com.nimbusds.jose.JOSEObjectType.JWT
import com.nimbusds.jose.JWSAlgorithm.HS256
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.CHILD_MODEL_NAME
import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.PARENT_MODEL_NAME
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.Date

object TestJwtFactory {
    private const val JWT_SECRET = "integration-test-jwt-secret-0123456789abcdef"

    fun createParentBearerToken(parentId: Long): String = createBearerToken(parentGlobalId(parentId))

    fun createChildBearerToken(childId: Long): String = createBearerToken(childGlobalId(childId))

    private fun createBearerToken(subject: String): String {
        val now = Instant.now()
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build()

        val signedJwt = SignedJWT(JWSHeader.Builder(HS256).type(JWT).build(), claims)
        signedJwt.sign(MACSigner(JWT_SECRET.toByteArray()))
        return signedJwt.serialize()
    }

    fun createParentAuthentication(
        parentId: Long,
        familyIds: List<Long> = emptyList(),
    ): AuthenticatedViewerAuthenticationToken =
        createAuthentication(
            subject = parentGlobalId(parentId),
            tokenId = "parent-$parentId",
            familyIds = familyIds,
            role = "ROLE_PARENT",
        )

    fun createChildAuthentication(
        childId: Long,
        familyIds: List<Long> = emptyList(),
    ): AuthenticatedViewerAuthenticationToken =
        createAuthentication(
            subject = childGlobalId(childId),
            tokenId = "child-$childId",
            familyIds = familyIds,
            role = "ROLE_CHILD",
        )

    private fun createAuthentication(
        subject: String,
        tokenId: String,
        familyIds: List<Long>,
        role: String,
    ): AuthenticatedViewerAuthenticationToken {
        val now = Instant.now()
        val jwt =
            Jwt
                .withTokenValue("test-token-$tokenId")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()

        val authorities = listOf(SimpleGrantedAuthority(role)) + familyIds.map { SimpleGrantedAuthority("FAMILY_$it") }

        return AuthenticatedViewerAuthenticationToken(
            jwt = jwt,
            authenticatedViewer = AuthenticatedViewerPrincipal(globalId = GlobalId.parse(subject)),
            authorities = authorities,
        )
    }

    private fun parentGlobalId(parentId: Long): String = globalId(PARENT_MODEL_NAME, parentId)

    private fun childGlobalId(childId: Long): String = globalId(CHILD_MODEL_NAME, childId)

    private fun globalId(
        modelName: String,
        id: Long,
    ): String = "gid://iou/$modelName/$id"
}
