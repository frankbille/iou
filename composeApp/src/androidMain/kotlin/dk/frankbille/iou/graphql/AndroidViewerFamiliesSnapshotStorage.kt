package dk.frankbille.iou.graphql

import android.content.Context
import java.io.File

internal object AndroidAppContextHolder {
    var applicationContext: Context? = null
}

private const val SNAPSHOT_FILENAME = "viewer-families.snapshot"

internal actual class ViewerFamiliesSnapshotStorage {
    actual fun load(): PersistedViewerFamiliesSnapshot? =
        snapshotFile()
            ?.takeIf(File::exists)
            ?.readText()
            ?.let(::decodePersistedViewerFamiliesSnapshot)

    actual fun save(snapshot: PersistedViewerFamiliesSnapshot) {
        snapshotFile()?.writeText(snapshot.encode())
    }

    actual fun clear() {
        snapshotFile()?.delete()
    }

    private fun snapshotFile(): File? =
        AndroidAppContextHolder.applicationContext
            ?.filesDir
            ?.resolve(SNAPSHOT_FILENAME)
}
