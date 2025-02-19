package mega.privacy.android.app.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.notifications.ScheduledMeetingPushMessageNotification
import mega.privacy.android.app.presentation.notifications.chat.ChatMessageNotification
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.data.mapper.pushmessage.PushMessageMapper
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.entity.pushes.PushMessage.*
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotifiableUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.notifications.GetChatMessageNotificationDataUseCase
import mega.privacy.android.domain.usecase.notifications.PushReceivedUseCase
import timber.log.Timber

/**
 * Worker class to manage push notifications.
 *
 * @property backgroundFastLoginUseCase        Required for performing a complete login process with an existing session.
 * @property pushReceivedUseCase                      Required for notifying received pushes.
 * @property retryPendingConnectionsUseCase    Required for retrying pending connections.
 * @property pushMessageMapper                 [PushMessageMapper].
 */
@HiltWorker
class PushMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val pushReceivedUseCase: PushReceivedUseCase,
    private val retryPendingConnectionsUseCase: RetryPendingConnectionsUseCase,
    private val pushMessageMapper: PushMessageMapper,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val scheduledMeetingPushMessageNotification: ScheduledMeetingPushMessageNotification,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val notificationManager: NotificationManagerCompat,
    private val isChatNotifiableUseCase: IsChatNotifiableUseCase,
    private val getChatRoom: GetChatRoom,
    private val fileDurationMapper: FileDurationMapper,
    private val getChatMessageNotificationDataUseCase: GetChatMessageNotificationDataUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        // legacy support, other places need to know logging in happen
        if (MegaApplication.isLoggingIn) {
            Timber.w("Logging already running.")
            return@withContext Result.failure()
        }

        MegaApplication.isLoggingIn = true
        val loginResult = runCatching { backgroundFastLoginUseCase() }
        MegaApplication.isLoggingIn = false

        if (loginResult.isSuccess) {
            Timber.d("Fast login success.")
            runCatching { retryPendingConnectionsUseCase(disconnect = false) }
                .recoverCatching { error ->
                    if (error is ChatNotInitializedErrorStatus) {
                        Timber.d("chat engine not ready. try to initialise megachat.")
                        initialiseMegaChatUseCase(loginResult.getOrDefault(""))
                    } else {
                        Timber.w(error)
                    }
                }.onFailure { error ->
                    Timber.e("Initialise MEGAChat failed: $error")
                    return@withContext Result.failure()
                }
        } else {
            Timber.e("Fast login error: ${loginResult.exceptionOrNull()}")
            return@withContext Result.failure()
        }

        when (val pushMessage = getPushMessageFromWorkerData(inputData)) {
            is ChatPushMessage -> {
                with(pushMessage) {
                    Timber.d("Should beep: $shouldBeep, Chat: $chatId, message: $msgId")

                    if (chatId == -1L || msgId == -1L) {
                        Timber.d("Message should be managed in onChatNotification")
                        return@withContext Result.success()
                    }

                    runCatching {
                        pushReceivedUseCase(shouldBeep, chatId)
                    }.onSuccess {
                        if (!isChatNotifiableUseCase(chatId) || !areNotificationsEnabled())
                            return@with

                        val data = getChatMessageNotificationDataUseCase(
                            shouldBeep,
                            chatId,
                            msgId,
                            DEFAULT_NOTIFICATION_URI.toString()
                        ) ?: return@withContext Result.failure()

                        ChatMessageNotification.show(
                            applicationContext,
                            data,
                            fileDurationMapper
                        )
                    }.onFailure { error ->
                        Timber.e(error)
                        return@withContext Result.failure()
                    }
                }
            }

            is ScheduledMeetingPushMessage -> {
                if (areNotificationsEnabled() && areMeetingRemindersEnabled()) {
                    runCatching {
                        scheduledMeetingPushMessageNotification.show(
                            applicationContext,
                            pushMessage.updateTitle()
                        )
                    }.onFailure { error ->
                        Timber.e(error)
                        return@withContext Result.failure()
                    }
                }
            }

            else -> {
                Timber.w("Unsupported Push Message type")
            }
        }

        Result.success()
    }

    /**
     * Get push message from worker input data
     *
     * @param data
     * @return          Push Message
     */
    private fun getPushMessageFromWorkerData(data: Data): PushMessage? =
        runCatching { pushMessageMapper(data) }
            .onFailure(Timber.Forest::e)
            .getOrNull()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = when (pushMessageMapper(inputData)) {
            is CallPushMessage -> getNotification(R.drawable.ic_call_started)
            is ChatPushMessage -> getNotification(
                R.drawable.ic_stat_notify,
                R.string.notification_chat_undefined_content
            )

            else -> getNotification(R.drawable.ic_stat_notify)
        }

        return ForegroundInfo(NOTIFICATION_CHANNEL_ID, notification)
    }

    private fun getNotification(iconId: Int, titleId: Int? = null): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                RETRIEVING_NOTIFICATIONS_ID,
                RETRIEVING_NOTIFICATIONS,
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        return NotificationCompat.Builder(applicationContext, RETRIEVING_NOTIFICATIONS_ID)
            .setSmallIcon(iconId)
            .apply {
                titleId?.let { setContentText(applicationContext.getString(titleId)) }
            }.build()
    }

    /**
     * Check if notifications are enabled and required permissions are granted
     *
     * @return  True if are enabled, false otherwise
     */
    private fun areNotificationsEnabled(): Boolean =
        notificationManager.areNotificationsEnabled() &&
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

    /**
     * Check if meeting reminders are enabled
     *
     * @return  True if are enabled, false otherwise
     */
    private suspend fun areMeetingRemindersEnabled(): Boolean =
        callsPreferencesGateway.getCallsMeetingRemindersPreference().firstOrNull() ==
                CallsMeetingReminders.Enabled

    private suspend fun ScheduledMeetingPushMessage.updateTitle(): ScheduledMeetingPushMessage =
        runCatching { getChatRoom(chatRoomHandle)?.title }.getOrNull()?.let { chatRoomTitle ->
            copy(title = chatRoomTitle)
        } ?: this

    companion object {
        const val NOTIFICATION_CHANNEL_ID = 1086
        const val RETRIEVING_NOTIFICATIONS_ID = "RETRIEVING_NOTIFICATIONS_ID"
        const val RETRIEVING_NOTIFICATIONS = "RETRIEVING_NOTIFICATIONS"
    }
}
