package dk.frankbille.iou.auth

import dk.frankbille.iou.parent.Parent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterParentRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 255)
    val password: String,
)

data class LoginParentRequest(
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String,
)

data class ParentAuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Instant,
    val parent: Parent,
)
