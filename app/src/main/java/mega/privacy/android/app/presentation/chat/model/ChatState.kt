package mega.privacy.android.app.presentation.chat.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus

/**
 * Chat UI state
 *
 * @property chatId                                     Chat Id.
 * @property schedId                                    Scheduled meeting Id.
 * @property error                                      String resource id for showing an error.
 * @property isCallAnswered                             Handle when a call is answered.
 * @property isChatInitialised                          True, if the chat is initialised. False, if not.
 * @property currentCallChatId                          Chat id of the call.
 * @property scheduledMeetingStatus                     [ScheduledMeetingStatus]
 * @property schedIsPending                             True, if scheduled meeting is pending. False, if not.
 * @property currentCallAudioStatus                     True, if audio is on. False, if audio is off.
 * @property currentCallVideoStatus                     True, if video is on. False, if video is off.
 * @property isPushNotificationSettingsUpdatedEvent     Push notification settings updated event
 * @property scheduledMeeting                           Scheduled Meeting.
 * @property titleChatArchivedEvent                     In case of a chat archived event, title of the chat.
 * @property isJoiningOrLeaving                         True if user is joining or leaving the chat, false otherwise.
 * @property joiningOrLeavingAction                     String ID which indicates if the UI to set is the joining or leaving state.
 * @property snackbarMessage                            String ID to display as a snackbar message.
 */
data class ChatState(
    val chatId: Long = -1L,
    val schedId: Long? = null,
    val error: Int? = null,
    val isCallAnswered: Boolean = false,
    val isChatInitialised: Boolean = false,
    val currentCallChatId: Long = -1L,
    val scheduledMeetingStatus: ScheduledMeetingStatus? = null,
    val schedIsPending: Boolean = false,
    val currentCallAudioStatus: Boolean = false,
    val currentCallVideoStatus: Boolean = false,
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
    val scheduledMeeting: ChatScheduledMeeting? = null,
    val titleChatArchivedEvent: String? = null,
    val isJoiningOrLeaving: Boolean = false,
    @StringRes val joiningOrLeavingAction: Int? = null,
    @StringRes val snackbarMessage: Int? = null,
)