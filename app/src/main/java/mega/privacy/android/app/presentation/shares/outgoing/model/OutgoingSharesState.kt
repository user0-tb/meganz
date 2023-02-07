package mega.privacy.android.app.presentation.shares.outgoing.model

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import nz.mega.sdk.MegaNode

/**
 * Outgoing shares UI state
 *
 * @param outgoingHandle current outgoing shares handle
 * @param outgoingTreeDepth current outgoing tree depth
 * @param outgoingParentHandle parent handle of the current outgoing node
 * @param nodes current list of nodes
 * @param isInvalidHandle true if handle is invalid
 * @param isLoading true if the nodes are loading
 * @param sortOrder current sort order
 * @param isMandatoryFingerprintVerificationNeeded Boolean to get if mandatory finger print verification Needed
 * @param unverifiedOutgoingShares List of unverified outgoing [ShareData]
 * @param unVerifiedOutgoingNodeHandles List of Unverified outgoing node handles
 * @param areUserCredentialsVerified Boolean value to read if user credentials are verified
 */
data class OutgoingSharesState(
    val outgoingHandle: Long = -1L,
    val outgoingTreeDepth: Int = 0,
    val outgoingParentHandle: Long? = null,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidHandle: Boolean = true,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isMandatoryFingerprintVerificationNeeded: Boolean = false,
    val unverifiedOutgoingShares: List<ShareData> = emptyList(),
    val unVerifiedOutgoingNodeHandles: List<Long> = emptyList(),
    val areUserCredentialsVerified: Boolean = false,
) {

    /**
     * Check if we are at the root of the outgoing shares page
     *
     * @return true if at the root of the outgoing shares page
     */
    fun isFirstNavigationLevel() = outgoingTreeDepth == 0
}