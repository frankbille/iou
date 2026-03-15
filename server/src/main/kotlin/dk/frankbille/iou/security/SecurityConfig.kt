package dk.frankbille.iou.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import java.nio.charset.StandardCharsets.UTF_8
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfig(
    private val authenticatedViewerJwtAuthenticationConverter: AuthenticatedViewerJwtAuthenticationConverter,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors {}
            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers(OPTIONS, "/graphql")
                    .permitAll()
                    .requestMatchers("/graphql")
                    .authenticated()
                    .anyRequest()
                    .denyAll()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(authenticatedViewerJwtAuthenticationConverter)
                }
            }.build()

    @Bean
    fun jwtDecoder(securityProperties: SecurityProperties): JwtDecoder {
        val key = securityProperties.jwtSecret.toByteArray(UTF_8)
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(HS256).build()
    }
}
