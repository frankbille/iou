package dk.frankbille.iou.graphql

import platform.Foundation.NSUserDefaults

private const val SNAPSHOT_KEY = "viewer-families.snapshot"

internal actual class ViewerSnapshotStorage {
    actual fun load(): PersistedViewerSnapshot? =
        NSUserDefaults.standardUserDefaults
            .stringForKey(SNAPSHOT_KEY)
            ?.let(::decodePersistedViewerSnapshot)

    actual fun save(snapshot: PersistedViewerSnapshot) {
        NSUserDefaults.standardUserDefaults.setObject(snapshot.encode(), SNAPSHOT_KEY)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(SNAPSHOT_KEY)
    }
}
