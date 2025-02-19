package mega.privacy.android.app.di.transfers

import mega.privacy.android.domain.di.TransferModule as DomainTransferModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.DefaultHasPendingUploads
import mega.privacy.android.domain.usecase.DefaultMonitorTransfersSize
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.HasPendingUploads
import mega.privacy.android.domain.usecase.MonitorTransfersSize
import mega.privacy.android.domain.usecase.ResetTotalDownloads

/**
 * Use cases to check on transfer status
 */
@Module(includes = [DomainTransferModule::class])
@InstallIn(SingletonComponent::class, ViewModelComponent::class, ServiceComponent::class)
abstract class TransfersModule {

    /**
     * Binds the Use Case [HasPendingUploads] to its default implementation [DefaultHasPendingUploads]
     *
     * @param useCase [DefaultHasPendingUploads]
     * @return [HasPendingUploads]
     */
    @Binds
    abstract fun bindHasPendingUploads(useCase: DefaultHasPendingUploads): HasPendingUploads

    /**
     * Binds the Use Case [MonitorTransfersSize] to its default implementation [DefaultMonitorTransfersSize]
     *
     * @param useCase [DefaultMonitorTransfersSize]
     * @return [MonitorTransfersSize]
     */
    @Binds
    abstract fun bindMonitorTransfersSize(useCase: DefaultMonitorTransfersSize): MonitorTransfersSize

    companion object {

        /**
         * Provides the [GetNumPendingUploads] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [GetNumPendingUploads]
         */
        @Provides
        fun provideGetNumPendingUploads(transfersRepository: TransferRepository): GetNumPendingUploads =
            GetNumPendingUploads(transfersRepository::getNumPendingUploads)

        /**
         * Provides the [GetNumPendingTransfers] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [GetNumPendingTransfers]
         */
        @Provides
        fun provideGetNumPendingTransfers(transfersRepository: TransferRepository): GetNumPendingTransfers =
            GetNumPendingTransfers(transfersRepository::getNumPendingTransfers)

        /**
         * Provides the [AreAllTransfersPaused] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [AreAllTransfersPaused]
         */
        @Provides
        fun provideAreAllTransfersPaused(transfersRepository: TransferRepository): AreAllTransfersPaused =
            AreAllTransfersPaused(transfersRepository::areAllTransfersPaused)

        /**
         * Provides the [ResetTotalDownloads] implementation
         *
         * @param transferRepository [TransferRepository]
         */
        @Provides
        fun provideResetTotalDownloads(transferRepository: TransferRepository):
                ResetTotalDownloads =
            ResetTotalDownloads(transferRepository::resetTotalDownloads)
    }
}
