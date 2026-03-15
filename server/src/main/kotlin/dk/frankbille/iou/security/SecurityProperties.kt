package dk.frankbille.iou.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("iou.security")
data class SecurityProperties(
    val jwtSecret: String,
    val accessTokenLifetime: Duration = Duration.ofDays(90),
)
