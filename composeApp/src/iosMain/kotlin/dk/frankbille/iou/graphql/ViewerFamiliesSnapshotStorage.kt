package dk.frankbille.iou.graphql

import platform.Foundation.NSUserDefaults

private const val SNAPSHOT_KEY = "viewer-families.snapshot"

internal actual class ViewerFamiliesSnapshotStorage {
    actual fun load(): PersistedViewerFamiliesSnapshot? =
        NSUserDefaults.standardUserDefaults
            .stringForKey(SNAPSHOT_KEY)
            ?.let(::decodePersistedViewerFamiliesSnapshot)

    actual fun save(snapshot: PersistedViewerFamiliesSnapshot) {
        NSUserDefaults.standardUserDefaults.setObject(snapshot.encode(), SNAPSHOT_KEY)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(SNAPSHOT_KEY)
    }
}
