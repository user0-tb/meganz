package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetOfflineFile

/**
 * Default Implementation of [GetImageByOfflineNodeHandle]
 */
class DefaultGetImageByOfflineNodeHandle(
    private val nodeRepository: NodeRepository,
    private val getOfflineFile: GetOfflineFile,
    private val imageRepository: ImageRepository,
) : GetImageByOfflineNodeHandle {
    override suspend fun invoke(
        nodeHandle: Long,
        highPriority: Boolean,
    ): ImageResult {
        nodeRepository.getOfflineNodeInformation(nodeHandle)?.let { nodeInformation ->
            getOfflineFile(nodeInformation).let { file ->
                if (!file.exists()) throw IllegalArgumentException("Offline file doesn't exist")
                return imageRepository.getImageByOfflineFile(
                    offlineNodeInformation = nodeInformation,
                    file = file,
                    highPriority = highPriority,
                )
            }
        } ?: throw IllegalArgumentException("Offline node was not found")
    }
}