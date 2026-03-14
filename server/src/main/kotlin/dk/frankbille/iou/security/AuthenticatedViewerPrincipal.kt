package dk.frankbille.iou.security

data class AuthenticatedViewerPrincipal(
    val globalId: GlobalId,
) {
    val subject: String
        get() = globalId.value

    val viewerId: Long
        get() = globalId.modelId

    val isParent: Boolean
        get() = globalId.modelName == PARENT_MODEL_NAME

    val isChild: Boolean
        get() = globalId.modelName == CHILD_MODEL_NAME

    val parentId: Long
        get() = requireRole(PARENT_MODEL_NAME)

    val childId: Long
        get() = requireRole(CHILD_MODEL_NAME)

    private fun requireRole(expectedModelName: String): Long {
        if (globalId.modelName != expectedModelName) {
            throw IllegalStateException("Authenticated viewer is not a ${expectedModelName.lowercase()}")
        }

        return globalId.modelId
    }

    companion object {
        const val PARENT_MODEL_NAME = "Parent"
        const val CHILD_MODEL_NAME = "Child"
    }
}
