package dk.frankbille.iou.graphql

internal data class PersistedViewerFamiliesSnapshot(
    val serverUrl: String,
    val tokenHash: Int,
    val payload: String,
)

internal fun PersistedViewerFamiliesSnapshot.encode(): String =
    buildString {
        append(serverUrl)
        append('\n')
        append(tokenHash)
        append('\n')
        append(payload)
    }

internal fun decodePersistedViewerFamiliesSnapshot(encoded: String): PersistedViewerFamiliesSnapshot? {
    val parts = encoded.split('\n', limit = 3)
    if (parts.size != 3) {
        return null
    }

    val tokenHash = parts[1].toIntOrNull() ?: return null
    return PersistedViewerFamiliesSnapshot(
        serverUrl = parts[0],
        tokenHash = tokenHash,
        payload = parts[2],
    )
}

internal expect class ViewerFamiliesSnapshotStorage() {
    fun load(): PersistedViewerFamiliesSnapshot?

    fun save(snapshot: PersistedViewerFamiliesSnapshot)

    fun clear()
}
