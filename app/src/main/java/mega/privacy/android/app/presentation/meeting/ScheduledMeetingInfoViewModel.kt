package mega.privacy.android.app.presentation.meeting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME
import mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.InviteContact
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import mega.privacy.android.domain.usecase.UpdateChatPermissions
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * ScheduledMeetingInfoActivity view model.
 *
 * @property getChatRoom                                    [GetChatRoom]
 * @property getChatParticipants                            [GetChatParticipants]
 * @property getScheduledMeetingByChat                      [GetScheduledMeetingByChat]
 * @property getChatCall                                    [GetChatCall]
 * @property getVisibleContactsUseCase                      [GetVisibleContactsUseCase]
 * @property queryChatLink                                  [QueryChatLink]
 * @property removeChatLink                                 [RemoveChatLink]
 * @property createChatLink                                 [CreateChatLink]
 * @property inviteToChat                                   [InviteToChat]
 * @property leaveChat                                      [LeaveChat]
 * @property removeFromChat                                 [RemoveFromChat]
 * @property inviteContact                                  [InviteContact]
 * @property setOpenInvite                                  [SetOpenInvite]
 * @property updateChatPermissions                          [UpdateChatPermissions]
 * @property getPublicChatToPrivate                         [SetPublicChatToPrivate]
 * @property getChatRoomUseCase                             [GetChatRoomUseCase]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property chatManagement                                 [ChatManagement]
 * @property startConversationUseCase                       [StartConversationUseCase]
 * @property openOrStartCall                                [OpenOrStartCall]
 * @property monitorChatListItemUpdates                     [MonitorChatListItemUpdates]
 * @property monitorScheduledMeetingUpdates                 [MonitorScheduledMeetingUpdates]
 * @property monitorConnectivityUseCase                     [MonitorConnectivityUseCase]
 * @property monitorChatRoomUpdates                         [MonitorChatRoomUpdates]
 * @property monitorUpdatePushNotificationSettingsUseCase   [MonitorUpdatePushNotificationSettingsUseCase]
 * @property cameraGateway                                  [CameraGateway]
 * @property deviceGateway                                  [DeviceGateway]
 * @property state                    Current view state as [ScheduledMeetingInfoState]

 */
@HiltViewModel
class ScheduledMeetingInfoViewModel @Inject constructor(
    private val getChatRoom: GetChatRoom,
    private val getChatParticipants: GetChatParticipants,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCall: GetChatCall,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val queryChatLink: QueryChatLink,
    private val removeChatLink: RemoveChatLink,
    private val createChatLink: CreateChatLink,
    private val inviteToChat: InviteToChat,
    private val leaveChat: LeaveChat,
    private val removeFromChat: RemoveFromChat,
    private val inviteContact: InviteContact,
    private val setOpenInvite: SetOpenInvite,
    private val updateChatPermissions: UpdateChatPermissions,
    private val getPublicChatToPrivate: SetPublicChatToPrivate,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatManagement: ChatManagement,
    private val startConversationUseCase: StartConversationUseCase,
    private val openOrStartCall: OpenOrStartCall,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorChatListItemUpdates: MonitorChatListItemUpdates,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val cameraGateway: CameraGateway,
    private val deviceGateway: DeviceGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledMeetingInfoState())
    val state: StateFlow<ScheduledMeetingInfoState> = _state

    private val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    private var scheduledMeetingId: Long = megaChatApiGateway.getChatInvalidHandle()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    init {
        monitorMutedChatsUpdates()
    }

    /**
     * Sets chat id and scheduled meeting id
     *
     * @param newChatId                 Chat id.
     * @param newScheduledMeetingId     Scheduled meeting id.
     */
    fun setChatId(newChatId: Long, newScheduledMeetingId: Long) {
        if (newChatId != megaChatApiGateway.getChatInvalidHandle() && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            scheduledMeetingId = newScheduledMeetingId
            getChat()
        }
    }

    /**
     * Get chat room
     */
    private fun getChat() =
        viewModelScope.launch {
            runCatching {
                getChatRoom(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Chat room does not exist, finish $exception")
                finishActivity()
            }.onSuccess { chat ->
                Timber.d("Chat room exists")
                chat?.apply {
                    if (isActive) {
                        Timber.d("Chat room is active")
                        _state.update {
                            it.copy(
                                chatId = chatId,
                                chatTitle = title,
                                isHost = ownPrivilege == ChatRoomPermission.Moderator,
                                isOpenInvite = isOpenInvite || ownPrivilege == ChatRoomPermission.Moderator,
                                enabledAllowNonHostAddParticipantsOption = isOpenInvite,
                                isPublic = isPublic
                            )
                        }

                        loadAllChatParticipants()

                        getScheduledMeeting(chatId)
                        updateDndSeconds(chatId)
                        updateRetentionTimeSeconds(retentionTime)

                        queryChatLink()

                        getChatRoomUpdates(chatId)
                        getScheduledMeetingUpdates()
                        getChatListItemUpdates()
                    } else {
                        Timber.d("Chat room is not active, finish")
                        finishActivity()
                    }
                }
            }
        }

    /**
     * Monitor muted chats updates
     */
    private fun monitorMutedChatsUpdates() = viewModelScope.launch {
        monitorUpdatePushNotificationSettingsUseCase().collectLatest {
            updateDndSeconds(state.value.chatId)
        }
    }

    /**
     * Load all chat participants
     */
    private fun loadAllChatParticipants() = viewModelScope.launch {
        runCatching {
            getChatParticipants(state.value.chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    Timber.d("Updated list of participants: list ${list.size}")
                    _state.update {
                        it.copy(participantItemList = list, numOfParticipants = list.size)
                    }
                    updateFirstAndSecondParticipants()
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Update first and last participants
     */
    private fun updateFirstAndSecondParticipants() {
        _state.value.participantItemList.let { list ->
            _state.update {
                it.copy(
                    firstParticipant = if (list.isNotEmpty()) list.first() else null,
                    secondParticipant = if (list.size > 1) list[1] else null
                )
            }
        }
    }

    /**
     * Get scheduled meeting
     *
     * @param chatId Chat id.
     */
    private fun getScheduledMeeting(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(chatId)
            }.onFailure { exception ->
                Timber.e("Scheduled meeting does not exist, finish $exception")
                finishActivity()
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (isMainScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                            updateScheduledMeeting(scheduledMeetReceived = scheduledMeetReceived)
                            return@forEach
                        }
                    }
                }
            }
        }

    /**
     * Get chat room updates
     *
     * @param chatId Chat id.
     */
    private fun getChatRoomUpdates(chatId: Long) =
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId).collectLatest { chat ->
                _state.update { state ->
                    with(state) {
                        val isHost = if (chat.hasChanged(ChatRoomChange.OwnPrivilege)) {
                            Timber.d("Changes in own privilege")
                            chat.ownPrivilege == ChatRoomPermission.Moderator
                        } else {
                            isHost
                        }
                        val isOpenInvite = if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                            Timber.d("Changes in open invite")
                            chat.isOpenInvite || isHost
                        } else {
                            isOpenInvite
                        }
                        val title = if (chat.hasChanged(ChatRoomChange.Title)) {
                            Timber.d("Changes in chat title")
                            chat.title
                        } else {
                            chatTitle
                        }
                        val isPublic = if (chat.hasChanged(ChatRoomChange.ChatMode)) {
                            Timber.d("Changes in chat mode, isPublic ${chat.isPublic}")
                            chat.isPublic
                        } else {
                            isPublic
                        }
                        val retentionTime = if (chat.hasChanged(ChatRoomChange.RetentionTime)) {
                            Timber.d("Changes in retention time")
                            getInstance().sendBroadcast(
                                Intent(ACTION_UPDATE_RETENTION_TIME)
                                    .putExtra(RETENTION_TIME, chat.retentionTime)
                            )

                            if (chat.retentionTime != Constants.DISABLED_RETENTION_TIME) chat.retentionTime
                            else null
                        } else {
                            retentionTimeSeconds
                        }
                        copy(
                            isHost = isHost,
                            isOpenInvite = isOpenInvite,
                            enabledAllowNonHostAddParticipantsOption = chat.isOpenInvite,
                            chatTitle = title,
                            isPublic = isPublic,
                            retentionTimeSeconds = retentionTime
                        )
                    }
                }
            }
        }

    /**
     * Update scheduled meeting
     *
     * @param scheduledMeetReceived [ChatScheduledMeeting]
     */
    private fun updateScheduledMeeting(scheduledMeetReceived: ChatScheduledMeeting) {
        _state.update {
            it.copy(
                scheduledMeeting = scheduledMeetReceived,
                is24HourFormat = is24HourFormat
            )
        }
    }

    /**
     * Check if is the current scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's same. False if not.
     */
    private fun isSameScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        state.value.chatId == scheduledMeet.chatId

    /**
     * Check if is main scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's the main scheduled meeting. False if not.
     */
    private fun isMainScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        scheduledMeet.parentSchedId == megaChatApiGateway.getChatInvalidHandle()

    /**
     * Get scheduled meeting updates
     */
    private fun getScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->
                if (!isSameScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                    return@collectLatest
                }

                if (!isMainScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                    return@collectLatest
                }

                Timber.d("Monitor scheduled meeting updated, changes ${scheduledMeetReceived.changes}")
                when (val changes = scheduledMeetReceived.changes) {
                    ScheduledMeetingChanges.NewScheduledMeeting -> updateScheduledMeeting(
                        scheduledMeetReceived = scheduledMeetReceived
                    )

                    ScheduledMeetingChanges.Title,
                    ScheduledMeetingChanges.Description,
                    ScheduledMeetingChanges.StartDate,
                    ScheduledMeetingChanges.EndDate,
                    -> state.value.scheduledMeeting?.let { schedMeet ->
                        if (scheduledMeetReceived.schedId == schedMeet.schedId) {
                            when (changes) {
                                ScheduledMeetingChanges.Title -> {
                                    _state.update { state ->
                                        state.copy(
                                            scheduledMeeting = state.scheduledMeeting?.copy(
                                                title = scheduledMeetReceived.title
                                            )
                                        )
                                    }
                                }

                                ScheduledMeetingChanges.Description -> {
                                    _state.update { state ->
                                        state.copy(
                                            scheduledMeeting = state.scheduledMeeting?.copy(
                                                description = scheduledMeetReceived.description
                                            )
                                        )
                                    }
                                }

                                ScheduledMeetingChanges.StartDate -> {
                                    _state.update { state ->
                                        state.copy(
                                            scheduledMeeting = state.scheduledMeeting?.copy(
                                                startDateTime = scheduledMeetReceived.startDateTime,
                                            )
                                        )
                                    }
                                }

                                ScheduledMeetingChanges.EndDate -> {
                                    _state.update { state ->
                                        state.copy(
                                            scheduledMeeting = state.scheduledMeeting?.copy(
                                                endDateTime = scheduledMeetReceived.endDateTime,
                                            )
                                        )
                                    }
                                }

                                else -> {}
                            }

                        }
                    }

                    else -> {}
                }
            }
        }


    /**
     * Get chat list item updates
     */
    private fun getChatListItemUpdates() =
        viewModelScope.launch {
            monitorChatListItemUpdates().collectLatest { item ->
                when (item.changes) {
                    ChatListItemChanges.LastMessage -> {
                        if (item.lastMessageType == ChatRoomLastMessage.PublicHandleCreate ||
                            item.lastMessageType == ChatRoomLastMessage.PublicHandleDelete
                        ) {
                            queryChatLink()
                        }
                    }

                    else -> {}
                }
            }
        }

    /**
     * Update seconds of Do not disturb mode
     *
     * @param id    Chat id.
     */
    private fun updateDndSeconds(id: Long) {
        getPushNotificationSettingManagement().pushNotificationSetting?.let { push ->
            if (push.isChatDndEnabled(id)) {
                _state.update {
                    it.copy(dndSeconds = push.getChatDnd(id))
                }

                return
            }
        }

        _state.update {
            it.copy(dndSeconds = null)
        }
    }

    /**
     * Update retention time seconds
     *
     * @param retentionTime    Retention time seconds
     */
    private fun updateRetentionTimeSeconds(retentionTime: Long) {
        if (retentionTime == Constants.DISABLED_RETENTION_TIME) {
            _state.update {
                it.copy(retentionTimeSeconds = null)
            }
        } else {
            _state.update {
                it.copy(retentionTimeSeconds = retentionTime)
            }
        }
    }

    /**
     * Check if there is an existing chat-link for an public chat
     */
    private fun queryChatLink() =
        viewModelScope.launch {
            runCatching {
                queryChatLink(state.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { request ->
                Timber.d("Query chat link successfully")
                _state.update {
                    it.copy(
                        enabledMeetingLinkOption = request.text != null,
                        meetingLink = request.text
                    )
                }
            }
        }

    /**
     * Remove chat link
     */
    private fun removeChatLink() =
        viewModelScope.launch {
            runCatching {
                removeChatLink(state.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                showSnackBar(R.string.general_text_error)
            }.onSuccess { _ ->
                Timber.d("Remove chat link successfully")
                _state.update { it.copy(enabledMeetingLinkOption = false, meetingLink = null) }
            }
        }

    /**
     * Create chat link
     */
    private fun createChatLink() =
        viewModelScope.launch {
            runCatching {
                createChatLink(state.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                showSnackBar(R.string.general_text_error)
            }.onSuccess { request ->
                _state.update {
                    it.copy(
                        enabledMeetingLinkOption = true,
                        meetingLink = request.text
                    )
                }
            }
        }

    /**
     * Edit scheduled meeting if there is internet connection, shows an error if not.
     */
    fun onEditTap() {
        Timber.d("Edit scheduled meeting")
    }

    /**
     * See more or less participants in the list.
     */
    fun onSeeMoreOrLessTap() =
        _state.update { state ->
            state.copy(seeMoreVisible = !state.seeMoreVisible)
        }

    /**
     * Add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onInviteParticipantsTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
            viewModelScope.launch {
                val contactList = getVisibleContactsUseCase()
                when {
                    contactList.isEmpty() -> {
                        _state.update {
                            it.copy(addParticipantsNoContactsDialog = true, openAddContact = false)
                        }
                    }

                    ChatUtil.areAllMyContactsChatParticipants(state.value.chatId) -> {
                        _state.update {
                            it.copy(
                                addParticipantsNoContactsLeftToAddDialog = true,
                                openAddContact = false
                            )
                        }
                    }

                    else -> {
                        _state.update {
                            it.copy(openAddContact = true)
                        }
                    }
                }
            }
        } else {
            showSnackBar(R.string.check_internet_connection_error)
        }
    }

    /**
     * Send message to a participant
     */
    fun onSendMsgTap() =
        state.value.selected?.let { participant ->
            if (isConnected) {
                viewModelScope.launch {
                    runCatching {
                        startConversationUseCase(
                            isGroup = false,
                            userHandles = listOf(participant.handle)
                        )
                    }.onFailure { exception ->
                        Timber.e(exception)
                        showSnackBar(R.string.general_text_error)
                    }.onSuccess { chatId ->
                        Timber.d("Open chat room")
                        openChatRoom(chatId)
                    }
                }
            } else {
                showSnackBar(R.string.check_internet_connection_error)
            }
        }


    /**
     * Start call with a participant
     */
    fun onStartCallTap() =
        state.value.selected?.let { participant ->
            if (isConnected) {
                viewModelScope.launch {
                    runCatching {
                        startConversationUseCase(
                            isGroup = false,
                            userHandles = listOf(participant.handle)
                        )
                    }.onFailure { exception ->
                        Timber.d(exception)
                        showSnackBar(R.string.general_text_error)
                    }.onSuccess { chatCallId ->
                        openOrStartChatCall(chatCallId)
                    }
                }
            } else {
                showSnackBar(R.string.check_internet_connection_error)
            }
        }

    /**
     * Open call or start a new call and open it
     *
     * @param chatCallId chat id
     */
    private fun openOrStartChatCall(chatCallId: Long) {
        cameraGateway.setFrontCamera()
        viewModelScope.launch {
            openOrStartCall(chatCallId, video = false, audio = true)?.let { call ->
                Timber.d("Call started")
                MegaApplication.isWaitingForCall = false
                CallUtil.addChecksForACall(call.chatId, false)
                if (call.isOutgoing) {
                    chatManagement.setRequestSentCall(call.callId, true)
                }
                passcodeManagement.showPasscodeScreen = true
                getInstance().openCallService(chatCallId)
                openChatCall(call.chatId)
            }
        }
    }

    /**
     * Change permissions
     */
    fun onChangePermissionsTap() =
        _state.value.selected?.let {
            showChangePermissionsDialog(it.privilege)
        }

    /**
     * Show or hide Remove participant dialog
     *
     * @param shouldShowDialog True,show dialog.
     */
    fun onRemoveParticipantTap(shouldShowDialog: Boolean) =
        _state.update {
            it.copy(openRemoveParticipantDialog = shouldShowDialog)
        }

    /**
     * Leave group chat button clicked
     */
    fun onLeaveGroupTap() =
        _state.update { state ->
            state.copy(leaveGroupDialog = !state.leaveGroupDialog)
        }

    /**
     * Invite contact
     */
    fun onInviteContactTap() =
        _state.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    inviteContact(participant.email)
                }.onFailure { exception ->
                    Timber.e(exception)
                    showSnackBar(R.string.general_error)
                }.onSuccess { request ->
                    when (request) {
                        InviteContactRequest.Sent -> showSnackBar(R.string.context_contact_request_sent)
                        InviteContactRequest.Resent -> showSnackBar(R.string.context_contact_invitation_resent)
                        InviteContactRequest.Deleted -> showSnackBar(R.string.context_contact_invitation_deleted)
                        InviteContactRequest.AlreadySent -> showSnackBar(R.string.invite_not_sent_already_sent)
                        InviteContactRequest.AlreadyContact -> showSnackBar(R.string.context_contact_already_exists)
                        InviteContactRequest.InvalidEmail -> showSnackBar(R.string.context_contact_already_exists)
                        else -> showSnackBar(R.string.general_error)
                    }
                }
            }
        }

    /**
     * Remove open add contact screen
     */
    fun removeAddContact() =
        _state.update {
            it.copy(openAddContact = null)
        }

    /**
     * Invite participants to the chat room
     *
     * @param contacts list of contacts
     */
    fun inviteToChat(contacts: ArrayList<String>) {
        Timber.d("Invite participants")
        viewModelScope.launch {
            inviteToChat(_state.value.chatId, contacts)
        }
        showSnackBar(R.string.invite_sent)
    }

    /**
     * Dismiss alert dialogs
     */
    fun dismissDialog() =
        _state.update { state ->
            state.copy(
                leaveGroupDialog = false,
                addParticipantsNoContactsDialog = false,
                addParticipantsNoContactsLeftToAddDialog = false
            )
        }

    /**
     * Finish activity
     */
    private fun finishActivity() =
        _state.update { state ->
            state.copy(finish = true)
        }

    /**
     * Leave chat
     */
    fun leaveChat() =
        viewModelScope.launch {
            runCatching {
                leaveChat(state.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                dismissDialog()
                showSnackBar(R.string.general_error)
            }.onSuccess { result ->
                Timber.d("Chat left ")
                if (result.userHandle == MegaApiJava.INVALID_HANDLE) {
                    result.chatHandle?.let { chatHandle ->
                        chatManagement.removeLeavingChatId(chatHandle)
                    }
                }

                dismissDialog()
                finishActivity()
            }
        }

    /**
     * Open bottom panel option of a participant.
     *
     * @param participant [ChatParticipant]
     */
    fun onParticipantTap(participant: ChatParticipant) =
        _state.update {
            it.copy(selected = participant)
        }

    /**
     * Create or removed meeting link if there is internet connection, shows an error if not.
     */
    fun onMeetingLinkTap() {
        if (isConnected) {
            Timber.d("Meeting link option")
            if (_state.value.enabledMeetingLinkOption) {
                removeChatLink()
            } else {
                createChatLink()
            }
        } else {
            showSnackBar(R.string.check_internet_connection_error)
        }
    }

    /**
     * Enable or disable the option Allow non-host add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAllowAddParticipantsTap() {
        if (isConnected) {
            Timber.d("Update option Allow non-host add participants to the chat room")
            viewModelScope.launch {
                runCatching {
                    setOpenInvite(state.value.chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    showSnackBar(R.string.general_text_error)
                }.onSuccess { result ->
                    _state.update {
                        it.copy(
                            isOpenInvite = result || it.isHost,
                            enabledAllowNonHostAddParticipantsOption = result
                        )
                    }
                }
            }
        } else {
            showSnackBar(R.string.check_internet_connection_error)
        }
    }

    /**
     * Enable encrypted key rotation if there is internet connection, shows an error if not.
     */
    fun enableEncryptedKeyRotation() {
        if (_state.value.participantItemList.size > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
            showSnackBar(R.string.warning_make_chat_private)
        } else {
            viewModelScope.launch {
                runCatching {
                    getPublicChatToPrivate(state.value.chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    showSnackBar(R.string.general_error)
                }.onSuccess { _ ->
                    _state.update { it.copy(isPublic = false) }
                }
            }
        }
    }

    /**
     * Open change selected participant permission
     *
     * @param selectedParticipantPermission [ChatRoomPermission]
     */
    fun showChangePermissionsDialog(selectedParticipantPermission: ChatRoomPermission?) {
        _state.update { it.copy(showChangePermissionsDialog = selectedParticipantPermission) }
    }

    /**
     * Open chat room
     *
     * @param chatId Chat id.
     */
    fun openChatRoom(chatId: Long?) =
        _state.update { it.copy(openChatRoom = chatId) }

    /**
     * Open chat call
     *
     * @param chatCallId Chat id.
     */
    fun openChatCall(chatCallId: Long?) {
        _state.update { it.copy(openChatCall = chatCallId) }
    }

    /**
     * Shares the link to chat
     *
     * @param data       Intent containing the info to share the content to chats.
     * @param action     Action to perform.
     */
    fun sendToChat(
        data: Intent?,
        action: (Intent?) -> Unit,
    ) {
        data?.putExtra(Constants.EXTRA_LINK, _state.value.meetingLink)
        action.invoke(data)
    }

    /**
     * Copy meeting link to clipboard
     *
     * @param clipboard [ClipboardManager]
     */
    fun copyMeetingLink(clipboard: ClipboardManager) {
        _state.value.meetingLink?.let { meetingLink ->
            val clip = ClipData.newPlainText(Constants.COPIED_TEXT_LABEL, meetingLink)
            clipboard.setPrimaryClip(clip)
            showSnackBar(R.string.scheduled_meetings_meeting_link_copied)
        }
    }

    /**
     * Update participant permissions
     *
     * @param permission [ChatRoomPermission]
     */
    fun updateParticipantPermissions(permission: ChatRoomPermission) =
        _state.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    updateChatPermissions(state.value.chatId, participant.handle, permission)
                }.onFailure { exception ->
                    Timber.e(exception)
                    showSnackBar(R.string.general_error)
                }.onSuccess {}
            }
        }

    /**
     * Remove selected participant from chat
     */
    fun removeSelectedParticipant() =
        _state.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    removeFromChat(state.value.chatId, participant.handle)
                }.onFailure { exception ->
                    Timber.e(exception)
                    showSnackBar(R.string.general_error)
                }.onSuccess {
                    showSnackBar(R.string.remove_participant_success)
                }
            }
        }

    /**
     * Show snackBar with a text
     *
     * @param stringId String id.
     */
    private fun showSnackBar(stringId: Int) {
        _state.update { it.copy(snackBar = stringId) }
    }

    /**
     * Updates state after shown snackBar.
     */
    fun snackbarShown() = _state.update { it.copy(snackBar = null) }

    /**
     * Open send to screen
     */
    fun openSendToChat(shouldOpen: Boolean) {
        _state.update { it.copy(openSendToChat = shouldOpen) }
    }

    /**
     * Check whether the initial bar should be displayed
     */
    fun checkInitialSnackbar(shouldBeShown: Boolean) {
        if (shouldBeShown) {
            showSnackBar(R.string.meetings_scheduled_meeting_info_snackbar_creating_scheduled_meeting_success)
        }
    }

    companion object {
        private const val MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100
    }
}