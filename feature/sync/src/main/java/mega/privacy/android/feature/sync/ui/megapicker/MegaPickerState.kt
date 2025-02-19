package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode

internal data class MegaPickerState(
    val currentFolder: Node? = null,
    val nodes: List<TypedNode>? = null,
)
