package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.EventMapper
import mega.privacy.android.data.mapper.UserAlertMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.GetScheduledMeeting
import mega.privacy.android.domain.usecase.meeting.FetchNumberOfScheduledMeetingOccurrencesByChat
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [NotificationsRepository]
 *
 * @property megaApiGateway
 */
internal class DefaultNotificationsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val userAlertsMapper: UserAlertMapper,
    private val eventMapper: EventMapper,
    private val fetchSchedOccurrencesByChatUseCase: FetchNumberOfScheduledMeetingOccurrencesByChat,
    private val getScheduledMeetingUseCase: GetScheduledMeeting,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
) : NotificationsRepository {

    override fun monitorUserAlerts() = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
        .mapNotNull { (newUserAlerts) ->
            withContext(dispatcher) {
                val userAlerts = newUserAlerts?.map { userAlert ->
                    userAlertsMapper(
                        userAlert,
                        ::provideContact,
                        ::provideScheduledMeeting,
                        ::provideSchedMeetingOccurrences,
                        megaApiGateway::getMegaNodeByHandle
                    )
                }

                if (!areMeetingInvitationsEnabled()) {
                    userAlerts?.filter { it !is ScheduledMeetingAlert }
                } else {
                    userAlerts
                }
            }
        }.flowOn(dispatcher)

    override fun monitorEvent(): Flow<Event> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnEvent>()
        .mapNotNull { (event) ->
            event?.let { eventMapper(it) }
        }

    override suspend fun getUserAlerts(): List<UserAlert> =
        withContext(dispatcher) {
            val userAlerts = megaApiGateway.getUserAlerts().map { userAlert ->
                userAlertsMapper(
                    userAlert,
                    ::provideContact,
                    ::provideScheduledMeeting,
                    ::provideSchedMeetingOccurrences,
                    megaApiGateway::getMegaNodeByHandle
                )
            }

            if (!areMeetingInvitationsEnabled()) {
                userAlerts.filter { it !is ScheduledMeetingAlert }
            } else {
                userAlerts
            }
        }

    private suspend fun provideEmail(userId: Long): String? =
        getEmailLocally(userId) ?: fetchAndCacheEmail(userId)

    private suspend fun fetchAndCacheEmail(userId: Long): String? =
        suspendCoroutine { continuation ->
            val callback = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request.email))
                    } else {
                        Timber.e("Error getting user email: ${error.errorString}")
                        continuation.resumeWith(Result.success(null))
                    }
                }
            )
            megaApiGateway.getUserEmail(userId, callback)
        }.also {
            it?.let { localStorageGateway.setNonContactEmail(userId, it) }
        }


    private suspend fun getEmailLocally(userId: Long) =
        localStorageGateway.getNonContactByHandle(userId)?.email

    private suspend fun provideContact(userId: Long, email: String?): Contact {
        val emailAddress = email ?: provideEmail(userId)
        val nickname = localStorageGateway.getContactByEmail(emailAddress)?.nickname
        val visible = isContactVisible(emailAddress)
        val hasPendingRequest = emailAddressFoundInPendingRequests(emailAddress)

        return Contact(
            userId = userId,
            email = emailAddress,
            nickname = nickname,
            isVisible = visible,
            hasPendingRequest = hasPendingRequest
        )
    }

    private suspend fun provideScheduledMeeting(
        chatId: Long,
        schedId: Long,
    ): ChatScheduledMeeting? = withContext(dispatcher) {
        runCatching { getScheduledMeetingUseCase(chatId, schedId) }.getOrNull()
    }

    private suspend fun provideSchedMeetingOccurrences(
        chatId: Long,
    ): List<ChatScheduledMeetingOccurr>? = withContext(dispatcher) {
        runCatching { fetchSchedOccurrencesByChatUseCase(chatId, 20) }.getOrNull()
    }

    private suspend fun emailAddressFoundInPendingRequests(emailAddress: String?) =
        megaApiGateway.getIncomingContactRequests()?.mapNotNull { it.sourceEmail }
            ?.contains(emailAddress) ?: false

    private suspend fun isContactVisible(emailAddress: String?) = emailAddress?.let {
        megaApiGateway.getContact(it)?.visibility == MegaUser.VISIBILITY_VISIBLE
    } ?: false

    override suspend fun acknowledgeUserAlerts() {
        megaApiGateway.acknowledgeUserAlerts()
    }

    override fun monitorHomeBadgeCount() = appEventGateway.monitorHomeBadgeCount()

    override suspend fun broadcastHomeBadgeCount(badgeCount: Int) = withContext(dispatcher) {
        appEventGateway.broadcastHomeBadgeCount(badgeCount)
    }

    private suspend fun areMeetingInvitationsEnabled(): Boolean =
        callsPreferencesGateway.getCallsMeetingInvitationsPreference().firstOrNull() ==
                CallsMeetingInvitations.Enabled
}