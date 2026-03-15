package dk.frankbille.iou.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebCorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/graphql")
            .allow()

        registry
            .addMapping("/auth/parents/login")
            .allow()

        registry
            .addMapping("/auth/parents/register")
            .allow()
    }

    private fun CorsRegistration.allow() =
        allowedOriginPatterns(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
        ).allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
}
