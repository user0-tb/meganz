package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder

/**
 * Use case interface for setting cloud sort order
 */
fun interface SetCloudSortOrder {

    /**
     * Set cloud sort order
     * @param order
     */
    suspend operator fun invoke(order: SortOrder)
}