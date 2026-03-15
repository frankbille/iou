package dk.frankbille.iou.graphql

import kotlinx.browser.window

private const val SERVER_URL_KEY = "iou.apollo.viewerFamilies.serverUrl"
private const val TOKEN_HASH_KEY = "iou.apollo.viewerFamilies.tokenHash"
private const val PAYLOAD_KEY = "iou.apollo.viewerFamilies.payload"

internal actual class ViewerSnapshotStorage {
    actual fun load(): PersistedViewerSnapshot? {
        val localStorage = window.localStorage
        val serverUrl = localStorage.getItem(SERVER_URL_KEY) ?: return null
        val tokenHash = localStorage.getItem(TOKEN_HASH_KEY)?.toIntOrNull() ?: return null
        val payload = localStorage.getItem(PAYLOAD_KEY) ?: return null
        return PersistedViewerSnapshot(
            serverUrl = serverUrl,
            tokenHash = tokenHash,
            payload = payload,
        )
    }

    actual fun save(snapshot: PersistedViewerSnapshot) {
        val localStorage = window.localStorage
        localStorage.setItem(SERVER_URL_KEY, snapshot.serverUrl)
        localStorage.setItem(TOKEN_HASH_KEY, snapshot.tokenHash.toString())
        localStorage.setItem(PAYLOAD_KEY, snapshot.payload)
    }

    actual fun clear() {
        val localStorage = window.localStorage
        localStorage.removeItem(SERVER_URL_KEY)
        localStorage.removeItem(TOKEN_HASH_KEY)
        localStorage.removeItem(PAYLOAD_KEY)
    }
}
