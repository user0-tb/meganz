package mega.privacy.android.app.di.sortorder

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.DefaultGetCameraSortOrder
import mega.privacy.android.app.domain.usecase.DefaultGetCloudSortOrder
import mega.privacy.android.app.domain.usecase.GetCameraSortOrder
import mega.privacy.android.app.domain.usecase.GetCloudSortOrder

/**
 * Provides the use case implementation regarding sort order
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SortOrderUseCases {

    /**
     * Provide the GetCloudSortOrder implementation
     */
    @Binds
    abstract fun bindGetCloudSortOrder(getCloudSortOrder: DefaultGetCloudSortOrder): GetCloudSortOrder

    @Binds
    abstract fun bindGetCameraSortOrder(getCameraSortOrder: DefaultGetCameraSortOrder): GetCameraSortOrder
}