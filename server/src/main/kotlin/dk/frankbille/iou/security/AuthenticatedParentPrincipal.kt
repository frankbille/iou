package dk.frankbille.iou.security

data class AuthenticatedParentPrincipal(
    val globalId: GlobalId,
) {
    val subject: String
        get() = globalId.value

    val parentId: Long
        get() = globalId.modelId
}
