package mega.privacy.android.app.presentation.inbox.model

import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * Inbox UI State
 *
 * @property currentViewType The current [ViewType]
 * @property hideMultipleItemSelection Whether to hide the Multiple Item Selection or not
 * @property inboxHandle The current Inbox Handle
 * @property nodes List of Inbox Nodes
 * @property shouldExitInbox Whether the User should leave the Inbox screen or not
 * @property triggerBackPress Whether the User has triggered a Back Press behavior or not
 * @property isPendingRefresh
 */
data class InboxState(
    val currentViewType: ViewType = ViewType.LIST,
    val hideMultipleItemSelection: Boolean = false,
    val inboxHandle: Long = -1L,
    val nodes: List<MegaNode> = emptyList(),
    val shouldExitInbox: Boolean = false,
    val triggerBackPress: Boolean = false,
    val isPendingRefresh: Boolean = false,
)