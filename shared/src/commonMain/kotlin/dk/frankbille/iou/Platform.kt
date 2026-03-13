package dk.frankbille.iou

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
