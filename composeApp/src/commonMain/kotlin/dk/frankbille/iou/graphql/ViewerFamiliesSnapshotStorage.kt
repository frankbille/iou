package dk.frankbille.iou.graphql

internal data class PersistedViewerSnapshot(
    val serverUrl: String,
    val tokenHash: Int,
    val payload: String,
)

internal fun PersistedViewerSnapshot.encode(): String =
    buildString {
        append(serverUrl)
        append('\n')
        append(tokenHash)
        append('\n')
        append(payload)
    }

internal fun decodePersistedViewerSnapshot(encoded: String): PersistedViewerSnapshot? {
    val parts = encoded.split('\n', limit = 3)
    if (parts.size != 3) {
        return null
    }

    val tokenHash = parts[1].toIntOrNull() ?: return null
    return PersistedViewerSnapshot(
        serverUrl = parts[0],
        tokenHash = tokenHash,
        payload = parts[2],
    )
}

internal expect class ViewerSnapshotStorage() {
    fun load(): PersistedViewerSnapshot?

    fun save(snapshot: PersistedViewerSnapshot)

    fun clear()
}
