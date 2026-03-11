package dk.frankbille.iou.security

import org.springframework.security.authentication.BadCredentialsException
import java.net.URI

data class GlobalId(
    val app: String,
    val modelName: String,
    val modelId: Long,
) {
    val value: String
        get() = "gid://$app/$modelName/$modelId"

    companion object {
        fun parse(value: String): GlobalId {
            val uri =
                try {
                    URI.create(value)
                } catch (exception: IllegalArgumentException) {
                    throw BadCredentialsException("Invalid subject claim", exception)
                }

            if (uri.scheme != "gid") {
                throw BadCredentialsException("Unsupported subject scheme: ${uri.scheme}")
            }

            val app = uri.host?.takeIf { it.isNotBlank() } ?: throw BadCredentialsException("Missing GlobalID app")
            val segments = uri.path?.trim('/')?.split('/')?.filter { it.isNotBlank() }.orEmpty()
            if (segments.size != 2) {
                throw BadCredentialsException("Invalid GlobalID path")
            }

            val modelId = segments[1].toLongOrNull() ?: throw BadCredentialsException("Invalid GlobalID model id")
            return GlobalId(app = app, modelName = segments[0], modelId = modelId)
        }
    }
}
