package dk.frankbille.iou.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("iou.security")
data class SecurityProperties(
    val jwtSecret: String,
)
