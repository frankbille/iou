package dk.frankbille.iou.auth

import com.nimbusds.jose.JWSAlgorithm.HS256
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.parent.toDto
import dk.frankbille.iou.security.AuthenticatedViewerPrincipal.Companion.PARENT_MODEL_NAME
import dk.frankbille.iou.security.GlobalId
import dk.frankbille.iou.security.SecurityProperties
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.Date
import java.util.Locale

@Service
class ParentAuthService(
    private val parentRepository: ParentRepository,
    private val parentAuthCredentialRepository: ParentAuthCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val securityProperties: SecurityProperties,
) {
    @Transactional
    fun register(request: RegisterParentRequest): ParentAuthResponse {
        val normalizedEmail = normalizeEmail(request.email)
        if (parentAuthCredentialRepository.findByEmail(normalizedEmail) != null) {
            throw ResponseStatusException(CONFLICT, "A parent account already exists for that email")
        }

        val name = request.name.trim()
        if (name.isBlank()) {
            throw ResponseStatusException(BAD_REQUEST, "Parent name must not be blank")
        }

        val parent =
            parentRepository.save(
                ParentEntity().apply {
                    this.name = name
                },
            )

        parentAuthCredentialRepository.save(
            ParentAuthCredentialEntity().apply {
                this.parent = parent
                email = normalizedEmail
                passwordHash = requireNotNull(passwordEncoder.encode(request.password))
            },
        )

        return issueToken(parent)
    }

    fun login(request: LoginParentRequest): ParentAuthResponse {
        val normalizedEmail = normalizeEmail(request.email)
        val credential =
            parentAuthCredentialRepository.findByEmail(normalizedEmail)
                ?: throw invalidCredentials()

        if (!passwordEncoder.matches(request.password, credential.passwordHash)) {
            throw invalidCredentials()
        }

        return issueToken(credential.parent)
    }

    private fun issueToken(parent: ParentEntity): ParentAuthResponse {
        val parentId = requireNotNull(parent.id)
        val now = Instant.now()
        val expiresAt = now.plus(securityProperties.accessTokenLifetime)
        val claims =
            JWTClaimsSet
                .Builder()
                .subject(GlobalId(app = "iou", modelName = PARENT_MODEL_NAME, modelId = parentId).value)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build()

        val signedJwt = SignedJWT(JWSHeader.Builder(HS256).build(), claims)
        signedJwt.sign(MACSigner(securityProperties.jwtSecret.toByteArray()))

        return ParentAuthResponse(
            accessToken = signedJwt.serialize(),
            expiresAt = expiresAt,
            parent = parent.toDto(),
        )
    }

    private fun normalizeEmail(email: String): String {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (normalizedEmail.isBlank()) {
            throw invalidEmail()
        }

        if (!EMAIL_PATTERN.matches(normalizedEmail)) {
            throw invalidEmail()
        }

        return normalizedEmail
    }

    private fun invalidEmail(): ResponseStatusException = ResponseStatusException(BAD_REQUEST, "Email must be a well-formed email address")

    private fun invalidCredentials(): ResponseStatusException = ResponseStatusException(UNAUTHORIZED, "Invalid email or password")

    companion object {
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
