package mega.privacy.android.data.di

import android.webkit.MimeTypeMap
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.mapper.AccountSessionDetailMapper
import mega.privacy.android.data.mapper.AccountStorageDetailMapper
import mega.privacy.android.data.mapper.AccountTransferDetailMapper
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.AchievementsOverviewMapper
import mega.privacy.android.data.mapper.BooleanPreferenceMapper
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.ContactRequestMapper
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.data.mapper.CountryMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.EventMapper
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.LocalPricingMapper
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MegaPurchaseMapper
import mega.privacy.android.data.mapper.MegaSkuMapper
import mega.privacy.android.data.mapper.MimeTypeMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.PaymentMethodTypeMapper
import mega.privacy.android.data.mapper.PaymentPlatformTypeMapper
import mega.privacy.android.data.mapper.PricingMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderIntMapperImpl
import mega.privacy.android.data.mapper.SortOrderMapper
import mega.privacy.android.data.mapper.SortOrderMapperImpl
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.mapper.SubscriptionOptionListMapper
import mega.privacy.android.data.mapper.SubscriptionStatusMapper
import mega.privacy.android.data.mapper.SyncStatusIntMapper
import mega.privacy.android.data.mapper.UserAlertMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.VideoAttachmentMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.VideoQualityIntMapper
import mega.privacy.android.data.mapper.VideoQualityMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapperImpl
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapperImpl
import mega.privacy.android.data.mapper.changepassword.PasswordStrengthMapper
import mega.privacy.android.data.mapper.changepassword.PasswordStrengthMapperImpl
import mega.privacy.android.data.mapper.getFileTypeInfo
import mega.privacy.android.data.mapper.getMimeType
import mega.privacy.android.data.mapper.mapBooleanPreference
import mega.privacy.android.data.mapper.mapMegaNodeListToNodeUpdate
import mega.privacy.android.data.mapper.mapMegaUserListToUserUpdate
import mega.privacy.android.data.mapper.storageStateToInt
import mega.privacy.android.data.mapper.syncStatusToInt
import mega.privacy.android.data.mapper.toAccountSessionDetail
import mega.privacy.android.data.mapper.toAccountStorageDetail
import mega.privacy.android.data.mapper.toAccountTransferDetail
import mega.privacy.android.data.mapper.toAccountType
import mega.privacy.android.data.mapper.toAchievementsOverview
import mega.privacy.android.data.mapper.toChatFilesFolderUserAttribute
import mega.privacy.android.data.mapper.toContactRequest
import mega.privacy.android.data.mapper.toCountry
import mega.privacy.android.data.mapper.toCountryCallingCodes
import mega.privacy.android.data.mapper.toDuration
import mega.privacy.android.data.mapper.toEvent
import mega.privacy.android.data.mapper.toImage
import mega.privacy.android.data.mapper.toLocalPricing
import mega.privacy.android.data.mapper.toMediaStoreFileTypeUri
import mega.privacy.android.data.mapper.toMegaAchievement
import mega.privacy.android.data.mapper.toMegaExceptionModel
import mega.privacy.android.data.mapper.toMegaPurchase
import mega.privacy.android.data.mapper.toMegaSku
import mega.privacy.android.data.mapper.toOfflineNodeInformation
import mega.privacy.android.data.mapper.toPaymentMethodType
import mega.privacy.android.data.mapper.toPaymentPlatformType
import mega.privacy.android.data.mapper.toPricing
import mega.privacy.android.data.mapper.toStorageState
import mega.privacy.android.data.mapper.toSubscriptionOptionList
import mega.privacy.android.data.mapper.toSubscriptionStatus
import mega.privacy.android.data.mapper.toUserAlert
import mega.privacy.android.data.mapper.toUserSet
import mega.privacy.android.data.mapper.toVideo
import mega.privacy.android.data.mapper.toVideoAttachment
import mega.privacy.android.data.mapper.toVideoQuality
import mega.privacy.android.data.mapper.verification.SmsPermissionMapper
import mega.privacy.android.data.mapper.verification.SmsPermissionMapperImpl
import mega.privacy.android.data.mapper.videoQualityToInt
import mega.privacy.android.data.mapper.viewtype.ViewTypeMapper
import mega.privacy.android.data.mapper.viewtype.ViewTypeMapperImpl
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MapperModule {

    @Binds
    abstract fun bindSmsPermissionMapper(implementation: SmsPermissionMapperImpl): SmsPermissionMapper

    @Binds
    abstract fun bindUploadOptionIntMapper(implementation: UploadOptionIntMapperImpl): UploadOptionIntMapper

    @Binds
    abstract fun bindViewTypeMapper(implementation: ViewTypeMapperImpl): ViewTypeMapper

    @Binds
    abstract fun bindCameraUploadsHandlesMapper(implementation: CameraUploadsHandlesMapperImpl): CameraUploadsHandlesMapper


    @Binds
    abstract fun bindSortOrderMapper(implementation: SortOrderMapperImpl): SortOrderMapper

    @Binds
    abstract fun bindSortOrderIntMapper(implementation: SortOrderIntMapperImpl): SortOrderIntMapper

    /**
     * Provides PasswordStrength Mapper
     */
    @Binds
    abstract fun bindsPasswordStrengthMapper(implementation: PasswordStrengthMapperImpl): PasswordStrengthMapper


    companion object {
        /**
         * Provide account type mapper
         */
        @Provides
        fun provideAccountTypeMapper(): AccountTypeMapper = ::toAccountType

        /**
         * Provide user update mapper
         */
        @Provides
        fun provideUserUpdateMapper(): UserUpdateMapper = ::mapMegaUserListToUserUpdate

        /**
         * Provide user alert mapper
         */
        @Provides
        fun provideUserAlertMapper(): UserAlertMapper = ::toUserAlert

        /**
         * Provide start screen mapper
         */
        @Provides
        fun provideStartScreenMapper(): StartScreenMapper = { StartScreen(it) }

        /**
         * Provide mega exception mapper
         */
        @Provides
        fun provideMegaExceptionMapper(): MegaExceptionMapper = ::toMegaExceptionModel

        /**
         * Provide images mapper
         */
        @Provides
        fun provideImagesMapper(): ImageMapper = ::toImage

        /**
         * Provide videos mapper
         */
        @Provides
        fun provideVideosMapper(): VideoMapper = ::toVideo

        /**
         * Provide event mapper
         */
        @Provides
        fun provideEventMapper(): EventMapper = ::toEvent

        /**
         * Provide media store file type uri mapper
         */
        @Provides
        fun provideMediaStoreFileTypeUriMapper(): MediaStoreFileTypeUriMapper =
            ::toMediaStoreFileTypeUri

        /**
         * Provide storage state mapper
         */
        @Provides
        fun provideStorageStateMapper(): StorageStateMapper = ::toStorageState

        /**
         * Provide [StorageState] to [Int] mapper
         */
        @Provides
        fun provideStorageStateIntMapper(): StorageStateIntMapper = ::storageStateToInt

        /**
         * Provide node update mapper
         */
        @Provides
        fun provideNodeUpdateMapper(): NodeUpdateMapper = ::mapMegaNodeListToNodeUpdate

        /**
         * Provide boolean preference mapper
         */
        @Provides
        fun provideBooleanPreferenceMapper(): BooleanPreferenceMapper = ::mapBooleanPreference

        /**
         * Provide file type info mapper
         *
         * @param mimeTypeMapper
         */
        @Provides
        fun provideFileTypeInfoMapper(mimeTypeMapper: MimeTypeMapper): FileTypeInfoMapper =
            { node ->
                getFileTypeInfo(node, mimeTypeMapper)
            }

        /**
         * Provide contact request mapper
         */
        @Provides
        fun provideContactRequestMapper(): ContactRequestMapper = ::toContactRequest

        /**
         * Provide mime type mapper
         */
        @Provides
        fun provideMimeTypeMapper(): MimeTypeMapper = { extension ->
            getMimeType(extension, MimeTypeMap.getSingleton()::getMimeTypeFromExtension)
        }

        /**
         * Provide local pricing mapper
         */
        @Provides
        fun provideLocalPricingMapper(): LocalPricingMapper = ::toLocalPricing

        /**
         * Provide currency mapper
         */
        @Provides
        fun provideCurrencyMapper(): CurrencyMapper = ::Currency

        /**
         * Provide paymentMethod type mapper
         */
        @Provides
        fun providePaymentMethodTypeMapper(): PaymentMethodTypeMapper = ::toPaymentMethodType

        /**
         * Provide subscription plan list mapper
         */
        @Provides
        fun provideSubscriptionOptionListMapper(): SubscriptionOptionListMapper =
            ::toSubscriptionOptionList

        /**
         * Provide mega achievement mapper
         */
        @Provides
        fun provideMegaAchievementMapper(): MegaAchievementMapper = ::toMegaAchievement

        /**
         * Provide mega achievements detail mapper
         */
        @Provides
        fun provideAchievementsOverviewMapper(): AchievementsOverviewMapper =
            ::toAchievementsOverview

        /**
         * Provide [UserSetMapper] mapper
         */
        @Provides
        fun provideUserSetMapper(): UserSetMapper = ::toUserSet


        /**
         * Provide [SubscriptionStatusMapper] mapper
         */
        @Provides
        fun provideSubscriptionStatusMapper(): SubscriptionStatusMapper = ::toSubscriptionStatus

        @Provides
        fun provideOfflineNodeInformationMapper(): OfflineNodeInformationMapper =
            ::toOfflineNodeInformation

        /**
         * Provide [CountryMapper] mapper
         */
        @Provides
        fun provideCountryMapper(): CountryMapper = ::toCountry

        /**
         * Provide country calling codes mapper
         */
        @Provides
        fun provideCountryCallingCodeMapper(): CountryCallingCodeMapper =
            CountryCallingCodeMapper(::toCountryCallingCodes)

        /**
         * Provide video quality mapper
         */
        @Provides
        fun provideVideoQualityMapper(): VideoQualityMapper = ::toVideoQuality

        /**
         * Provide pricing mapper
         *
         */
        @Provides
        fun providePricingMapper(): PricingMapper = ::toPricing

        /**
         * Provide video quality int mapper
         */
        @Provides
        fun provideVideoQualityIntMapper(): VideoQualityIntMapper = ::videoQualityToInt

        @Provides
        fun provideSyncStatusIntMapper(): SyncStatusIntMapper = ::syncStatusToInt

        @Provides
        fun provideMegaSkuMapper(): MegaSkuMapper = ::toMegaSku

        @Provides
        fun provideMegaPurchaseMapper(): MegaPurchaseMapper = ::toMegaPurchase

        @Provides
        fun provideChatFilesFolderUserAttributeMapper(): ChatFilesFolderUserAttributeMapper =
            ::toChatFilesFolderUserAttribute

        /**
         * Provide subscription platform type mapper
         */
        @Provides
        fun provideSubscriptionPlatformTypeMapper(): PaymentPlatformTypeMapper =
            ::toPaymentPlatformType

        /**
         * Provide account transfer detail mapper
         */
        @Provides
        fun provideAccountTransferDetailMapper(): AccountTransferDetailMapper =
            ::toAccountTransferDetail

        /**
         * Provide account session detail mapper
         */
        @Provides
        fun provideAccountSessionDetailMapper(): AccountSessionDetailMapper =
            ::toAccountSessionDetail

        /**
         * Provide account storage detail mapper
         */
        @Provides
        fun provideAccountStorageDetailMapper(): AccountStorageDetailMapper =
            ::toAccountStorageDetail

        /**
         * Provide file duration mapper
         */
        @Provides
        fun provideFileDurationMapper(): FileDurationMapper = ::toDuration

        /**
         * Provides VideoAttachment mapper
         */
        @Provides
        fun provideVideoAttachmentMapper(): VideoAttachmentMapper = ::toVideoAttachment

        /**
         * Provide gson builder
         */
        @Provides
        fun provideGsonBuilder(): GsonBuilder = GsonBuilder()
    }
}
