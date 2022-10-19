package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.FileTypeInfo

/**
 * File node
 */
interface FileNode : Node {
    /**
     * Size
     */
    val size: Long

    /**
     * Modification time
     */
    val modificationTime: Long

    /**
     * Type
     */
    val type: FileTypeInfo

    /**
     * Thumbnail path
     */
    val thumbnailPath: String?
}