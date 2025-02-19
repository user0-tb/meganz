package mega.privacy.android.data.model.node

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId

internal data class DefaultFileNode(
    override val id: NodeId,
    override val name: String,
    override val parentId: NodeId,
    override val base64Id: String,
    override val size: Long,
    override val label: Int,
    override val creationTime: Long,
    override val modificationTime: Long,
    override val hasVersion: Boolean,
    override val type: FileTypeInfo,
    override val thumbnailPath: String? = null,
    override val previewPath: String? = null,
    override val fullSizePath: String? = null,
    override val isFavourite: Boolean,
    override val exportedData: ExportedData?,
    override val isTakenDown: Boolean,
    override val isIncomingShare: Boolean,
    override val fingerprint: String?,
    override val originalFingerprint: String?,
    override val isNodeKeyDecrypted: Boolean,
    override val hasThumbnail: Boolean,
    override val hasPreview: Boolean,
) : FileNode
