package mega.privacy.android.app.presentation.photos.albums.importlink

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerSetMaxBufferSizeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import java.io.File
import javax.inject.Inject

/**
 * AlbumImport Preview help class
 */
class AlbumImportPreviewProvider @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerSetMaxBufferSizeUseCase: MegaApiHttpServerSetMaxBufferSizeUseCase,
    private val getAlbumPhotoFileUrlByNodeIdUseCase: GetAlbumPhotoFileUrlByNodeIdUseCase,
) {

    /**
     * onPreviewPhoto
     */
    fun onPreviewPhoto(
        activity: Activity,
        photo: Photo,
    ) {
        if (photo is Photo.Video) {
            (activity as LifecycleOwner).lifecycleScope.launch {
                launchVideoScreen(activity, photo)
            }
        } else {
            ImageViewerActivity.getIntentForAlbumSharing(
                activity,
                photo.id,
            ).run {
                activity.startActivity(this)
            }
            activity.overridePendingTransition(0, 0)
        }
    }

    /**
     * Launch video player
     *
     * @param activity
     * @param photo Photo item
     */
    private suspend fun launchVideoScreen(activity: Activity, photo: Photo) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        val intent = Util.getMediaIntent(activity, nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(
                Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                Constants.FROM_ALBUM_SHARING
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                getNodeParentHandle(nodeHandle)
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        activity.startActivity(
            isLocalFile(nodeHandle)?.let { localPath ->
                File(localPath).let { mediaFile ->
                    kotlin.runCatching {
                        FileProvider.getUriForFile(
                            activity,
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        )
                    }.onFailure {
                        Uri.fromFile(mediaFile)
                    }.map { mediaFileUri ->
                        intent.setDataAndType(
                            mediaFileUri,
                            MimeTypeList.typeForName(nodeName).type
                        )
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                }
                intent
            } ?: let {
                val memoryInfo = ActivityManager.MemoryInfo()
                (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .getMemoryInfo(memoryInfo)
                updateIntent(
                    handle = nodeHandle,
                    name = nodeName,
                    isNeedsMoreBufferSize = memoryInfo.totalMem > Constants.BUFFER_COMP,
                    intent = intent,
                )
            }
        )
    }

    /**
     * Get node parent handle
     *
     * @param handle node handle
     * @return parent handle
     */
    private suspend fun getNodeParentHandle(handle: Long): Long? =
        getNodeByHandle(handle)?.parentHandle

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    private suspend fun isLocalFile(
        handle: Long,
    ): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = FileUtil.getLocalFile(node)
            File(FileUtil.getDownloadLocation(), node.name).let { file ->
                if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                            || (node.fingerprint == getFingerprintUseCase(localPath)))
                ) {
                    localPath
                } else {
                    null
                }
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param isNeedsMoreBufferSize true is that sets 32MB, otherwise is false
     * @param intent Intent
     * @return updated intent
     */
    private suspend fun updateIntent(
        handle: Long,
        name: String,
        isNeedsMoreBufferSize: Boolean,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        megaApiHttpServerSetMaxBufferSizeUseCase(
            if (isNeedsMoreBufferSize) {
                Constants.MAX_BUFFER_32MB
            } else {
                Constants.MAX_BUFFER_16MB
            }
        )
        getAlbumPhotoFileUrlByNodeIdUseCase(NodeId(handle))?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(name).type)
            }
        }
        return intent
    }
}