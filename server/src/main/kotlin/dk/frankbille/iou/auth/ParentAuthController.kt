package dk.frankbille.iou.auth

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/parents")
class ParentAuthController(
    private val parentAuthService: ParentAuthService,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterParentRequest,
    ): ParentAuthResponse = parentAuthService.register(request)

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginParentRequest,
    ): ParentAuthResponse = parentAuthService.login(request)
}
