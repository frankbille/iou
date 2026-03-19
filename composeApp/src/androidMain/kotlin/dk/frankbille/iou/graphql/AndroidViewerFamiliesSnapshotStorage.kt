package dk.frankbille.iou.graphql

import android.content.Context
import java.io.File

internal object AndroidAppContextHolder {
    var applicationContext: Context? = null
}

private const val SNAPSHOT_FILENAME = "viewer-families.snapshot"

fun initializeAndroidAppContext(context: Context) {
    AndroidAppContextHolder.applicationContext = context
}

internal actual class ViewerSnapshotStorage {
    actual fun load(): PersistedViewerSnapshot? =
        snapshotFile()
            ?.takeIf(File::exists)
            ?.readText()
            ?.let(::decodePersistedViewerSnapshot)

    actual fun save(snapshot: PersistedViewerSnapshot) {
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
