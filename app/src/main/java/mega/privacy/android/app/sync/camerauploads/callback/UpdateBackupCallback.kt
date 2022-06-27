package mega.privacy.android.app.sync.camerauploads.callback

import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber

/**
 * Update backup event callback.
 */
class UpdateBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_PUT

    override fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
        // Update local cache.
        request.apply {
            val backup = getDatabase().getBackupById(parentHandle)

            if (backup != null && !backup.outdated) {
                backup.apply {
                    if (nodeHandle != MegaApiJava.INVALID_HANDLE) targetNode = nodeHandle
                    if (file != null) localFolder = file
                    if (access != Constants.INVALID_VALUE) state = access
                    if (name != null) backupName = name
                }
                getDatabase().updateBackup(backup)
                Timber.d("Successful callback: update $backup.")
            }
        }
    }
}