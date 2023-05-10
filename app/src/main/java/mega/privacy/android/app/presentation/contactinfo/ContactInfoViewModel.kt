package mega.privacy.android.app.presentation.contactinfo

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.isAwayOrOffline
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.meeting.ChatCallChanges.OnHold
import mega.privacy.android.domain.entity.meeting.ChatCallChanges.Status
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.meeting.TermCodeType
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.CreateChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.ApplyContactUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.domain.usecase.contact.SetUserAliasUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.GetInSharesUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactInfoActivity]
 *
 * @property monitorStorageStateEventUseCase    [MonitorStorageStateEventUseCase]
 * @property monitorConnectivityUseCase         [MonitorConnectivityUseCase]
 * @property startChatCall                      [StartChatCall]
 * @property getChatRoomUseCase                 [GetChatRoomUseCase]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property chatApiGateway                     [MegaChatApiGateway]
 * @property cameraGateway                      [CameraGateway]
 * @property chatManagement                     [ChatManagement]
 * @property monitorContactUpdates              [MonitorContactUpdates]
 * @property requestLastGreen                   [RequestLastGreen]
 * @property getChatRoom                        [GetChatRoom]
 * @property getUserOnlineStatusByHandleUseCase [GetUserOnlineStatusByHandleUseCase]
 * @property getChatRoomByUserUseCase           [GetChatRoomByUserUseCase]
 * @property getContactFromChatUseCase          [GetContactFromChatUseCase]
 * @property getContactFromEmailUseCase         [GetContactFromEmailUseCase]
 * @property applyContactUpdatesUseCase         [ApplyContactUpdatesUseCase]
 * @property setUserAliasUseCase                [SetUserAliasUseCase]
 * @property removeContactByEmailUseCase        [RemoveContactByEmailUseCase]
 * @property getInSharesUseCase                 [GetInSharesUseCase]
 */
@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val startChatCall: StartChatCall,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val requestLastGreen: RequestLastGreen,
    private val getChatRoom: GetChatRoom,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val getContactFromChatUseCase: GetContactFromChatUseCase,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val applyContactUpdatesUseCase: ApplyContactUpdatesUseCase,
    private val setUserAliasUseCase: SetUserAliasUseCase,
    private val removeContactByEmailUseCase: RemoveContactByEmailUseCase,
    private val getInSharesUseCase: GetInSharesUseCase,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val startConversationUseCase: StartConversationUseCase,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val createShareKey: CreateShareKey,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : BaseRxViewModel() {

    private val _state = MutableStateFlow(ContactInfoState())

    /**
     * UI State ContactInfo
     * Flow of [ContactInfoState]
     */
    val state = _state.asStateFlow()

    /**
     * Parent Handle
     */
    var parentHandle: Long? = null

    /**
     * Checks if contact info launched from contacts screen
     */
    val isFromContacts: Boolean
        get() = state.value.isFromContacts

    /**
     * User status
     */
    val userStatus: UserStatus
        get() = state.value.userStatus

    /**
     * User status
     */
    val userHandle: Long?
        get() = state.value.contactItem?.handle

    /**
     * User email
     */
    val userEmail: String?
        get() = state.value.contactItem?.email

    /**
     * chat id
     */
    val chatId: Long?
        get() = state.value.chatRoom?.chatId


    /**
     * Nick name
     */
    val nickName: String?
        get() = state.value.contactItem?.contactData?.alias

    init {
        getContactUpdates()
        monitorCallUpdates()
        monitorChatSessionUpdates()
        chatMuteUpdates()
        monitorNodeChanges()
    }

    private fun monitorNodeChanges() = viewModelScope.launch {
        monitorNodeUpdates()
            .filter { nodeUpdate ->
                nodeUpdate.changes.keys.any { node -> node.isIncomingShare }
                        || nodeUpdate.changes.values.any { it.contains(NodeChanges.Remove) }
            }.conflate().collect {
                getInShares()
            }
    }

    private fun chatMuteUpdates() = viewModelScope.launch {
        monitorUpdatePushNotificationSettingsUseCase().collect {
            _state.update { it.copy(isPushNotificationSettingsUpdated = true) }
        }
    }

    private fun monitorChatSessionUpdates() = viewModelScope.launch {
        monitorChatSessionUpdatesUseCase().takeWhile {
            it.changes in listOf(
                ChatSessionChanges.Status,
                ChatSessionChanges.RemoteAvFlags,
                ChatSessionChanges.SessionSpeakRequested,
                ChatSessionChanges.SessionOnHiRes,
                ChatSessionChanges.SessionOnLowRes,
                ChatSessionChanges.SessionOnHold,
                ChatSessionChanges.AudioLevel,
            )
        }.collect {
            _state.update { it.copy(callStatusChanged = true) }
        }
    }

    private fun monitorCallUpdates() = viewModelScope.launch {
        monitorChatCallUpdates()
            .filter { it.changes in listOf(Status, OnHold) }
            .collect { call ->
                if (call.changes == Status) observeCallStatus(call)
                else if (call.changes == OnHold) _state.update { it.copy(callStatusChanged = true) }
            }
    }

    private fun observeCallStatus(call: ChatCall) {
        if (call.status in listOf(
                ChatCallStatus.Connecting,
                ChatCallStatus.InProgress,
                ChatCallStatus.Destroyed,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.UserNoPresent
            )
        ) {
            _state.update { it.copy(callStatusChanged = true) }
            if (call.status == ChatCallStatus.TerminatingUserParticipation &&
                call.termCode == TermCodeType.TooManyParticipants
            ) {
                _state.update { it.copy(snackBarMessage = R.string.call_error_too_many_participants) }
            }
        }
    }

    /**
     * Monitors contact changes.
     */
    private fun getContactUpdates() = viewModelScope.launch {
        monitorContactUpdates().collectLatest { updates ->
            val userInfo = state.value.contactItem?.let { applyContactUpdatesUseCase(it, updates) }
            if (updates.changes.containsValue(listOf(UserChanges.Avatar))) {
                updateAvatar(userInfo)
            } else {
                _state.update { it.copy(contactItem = userInfo) }
            }
        }
    }

    /**
     * Refreshes user info
     *
     * User online status, Credential verification, User info change will be updated
     */
    fun refreshUserInfo() = viewModelScope.launch {
        val email = state.value.email ?: return@launch
        runCatching {
            getContactFromEmailUseCase(email, isOnline())
        }.onSuccess {
            _state.update { state -> state.copy(contactItem = it) }
            it?.handle?.let { handle -> runCatching { requestLastGreen(handle) } }
        }
    }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = monitorConnectivityUseCase().value

    /**
     * Starts a call
     *
     * @param chatId Chat id
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    fun onCallTap(chatId: Long, video: Boolean, audio: Boolean) {
        if (chatApiGateway.getChatCall(chatId) != null) {
            _state.update { it.copy(isCallStarted = true) }

            Timber.d("There is a call, open it")
            CallUtil.openMeetingInProgress(
                MegaApplication.getInstance().applicationContext, chatId, true, passcodeManagement
            )
            return
        }

        MegaApplication.isWaitingForCall = false

        cameraGateway.setFrontCamera()

        viewModelScope.launch {
            runCatching {
                startChatCall(chatId, video, audio)
            }.onFailure { exception ->
                _state.update { it.copy(error = R.string.call_error) }
                Timber.e(exception)
            }.onSuccess { resultStartCall ->
                _state.update { it.copy(isCallStarted = true) }
                val resultChatId = resultStartCall.chatHandle
                if (resultChatId != null) {
                    val videoEnable = resultStartCall.flag
                    val paramType = resultStartCall.paramType
                    val audioEnable: Boolean = paramType == ChatRequestParamType.Video

                    CallUtil.addChecksForACall(resultChatId, videoEnable)

                    chatApiGateway.getChatCall(resultChatId)?.let { call ->
                        if (call.isOutgoing) {
                            chatManagement.setRequestSentCall(call.callId, true)
                        }
                    }

                    CallUtil.openMeetingWithAudioOrVideo(
                        MegaApplication.getInstance().applicationContext,
                        resultChatId,
                        audioEnable,
                        videoEnable,
                        passcodeManagement
                    )
                }
            }
        }
    }

    /**
     * Gets user online status by user handle
     * Requests for lastGreen
     */
    fun getUserStatusAndRequestForLastGreen() = viewModelScope.launch {
        val handle = state.value.contactItem?.handle ?: return@launch
        runCatching { getUserOnlineStatusByHandleUseCase(handle) }.onSuccess { status ->
            if (status.isAwayOrOffline()) {
                requestLastGreen(handle)
            }
            _state.update { it.copy(userStatus = status) }
        }
    }

    /**
     * Method updates the last green status to contact info state
     *
     * @param userHandle user handle of the user
     * @param lastGreen last green status
     */
    fun updateLastGreen(userHandle: Long, lastGreen: Int) = viewModelScope.launch {
        runCatching {
            getUserOnlineStatusByHandleUseCase(userHandle = userHandle)
        }.onSuccess { status ->
            _state.update {
                it.copy(
                    lastGreen = lastGreen,
                    userStatus = status,
                )
            }
        }
    }

    /**
     * Method to get chat room and contact info
     *
     * @param chatHandle chat handle of selected chat
     * @param email email id of selected user
     */
    fun updateContactInfo(chatHandle: Long, email: String? = null) = viewModelScope.launch {
        val isFromContacts = chatHandle == -1L
        _state.update { it.copy(isFromContacts = isFromContacts) }
        var userInfo: ContactItem? = null
        var chatRoom: ChatRoom? = null
        if (isFromContacts) {
            runCatching {
                email?.let { mail -> getContactFromEmailUseCase(mail, isOnline()) }
            }.onSuccess { contact ->
                userInfo = contact
                chatRoom = runCatching {
                    contact?.handle?.let { getChatRoomByUserUseCase(it) }
                }.getOrNull()
            }
        } else {
            runCatching {
                chatRoom = getChatRoom(chatHandle)
                userInfo = getContactFromChatUseCase(chatHandle, isOnline())
            }
        }

        _state.update {
            it.copy(
                lastGreen = userInfo?.lastSeen ?: 0,
                userStatus = userInfo?.status ?: UserStatus.Invalid,
                contactItem = userInfo,
                chatRoom = chatRoom,
            )
        }
        updateAvatar(userInfo)
        getInShares()
    }

    private fun updateAvatar(userInfo: ContactItem?) = viewModelScope.launch(ioDispatcher) {
        val avatarFile = userInfo?.contactData?.avatarUri?.let { File(it) }
        val avatar = AvatarUtil.getAvatarBitmap(avatarFile)
        _state.update { it.copy(avatar = avatar) }
    }

    /**
     * Sets parent handle
     *
     * @param handle parent handle
     */
    fun setParentHandle(handle: Long) {
        parentHandle = handle
    }

    /**
     * Method to update the nick name of the user
     *
     * @param newNickname new nick name given by the user
     */
    fun updateNickName(newNickname: String?) = viewModelScope.launch {
        val handle = state.value.contactItem?.handle
        if (handle == null || (nickName != null && nickName == newNickname)) return@launch
        runCatching {
            setUserAliasUseCase(newNickname, handle)
        }.onSuccess { alias ->
            _state.update {
                it.copy(
                    snackBarMessage = alias?.let { R.string.snackbar_nickname_added }
                        ?: run { R.string.snackbar_nickname_removed },
                )
            }
        }
    }

    /**
     * on Consume Snack Bar Message event
     */
    fun onConsumeSnackBarMessageEvent() {
        viewModelScope.launch {
            _state.update { it.copy(snackBarMessage = null, snackBarMessageString = null) }
        }
    }

    /**
     * on Consume chat call status change
     */
    fun onConsumeChatCallStatusChangeEvent() {
        viewModelScope.launch {
            _state.update { it.copy(callStatusChanged = false) }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdated = false) }
        }
    }


    /**
     * on Consume navigate to chat activity event
     */
    fun onConsumeNavigateToChatEvent() {
        viewModelScope.launch {
            _state.update { it.copy(shouldNavigateToChat = false) }
        }
    }

    /**
     * on Consume chat notification change event
     */
    fun onConsumeChatNotificationChangeEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isChatNotificationChange = false) }
        }
    }

    /**
     * on Consume storage over quota event
     */
    fun onConsumeStorageOverQuotaEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isStorageOverQuota = false) }
        }
    }

    /**
     * on Consume node update event
     */
    fun onConsumeNodeUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isNodeUpdated = false) }
        }
    }

    /**
     * on Consume is transfer complete
     */
    fun onConsumeIsTransferComplete() {
        viewModelScope.launch {
            _state.update { it.copy(isTransferComplete = false) }
        }
    }

    /**
     * on Consume copy exception
     */
    fun onConsumeCopyException() {
        viewModelScope.launch {
            _state.update { it.copy(copyError = null) }
        }
    }

    /**
     * on Consume copy exception
     */
    fun onConsumeNameCollisions() {
        viewModelScope.launch {
            _state.update { it.copy(nameCollisions = emptyList()) }
        }
    }

    /**
     * Remove selected contact from user account
     * InShares are removed and existing calls are closed
     * Exits from contact info page if succeeds
     */
    fun removeContact() = applicationScope.launch {
        val isRemoved = state.value.email?.let { email -> removeContactByEmailUseCase(email) }
        _state.update {
            it.copy(isUserRemoved = isRemoved ?: false)
        }
    }

    /**
     * Gets the in shared nodes for the user
     * value is updated to contact info state
     */
    fun getInShares() = viewModelScope.launch {
        val email = state.value.email ?: return@launch
        val inShares = getInSharesUseCase(email)
        if (inShares == state.value.inShares) return@launch
        parentHandle = inShares.firstOrNull()?.parentId?.longValue ?: INVALID_NODE_HANDLE
        _state.update { it.copy(inShares = inShares, isNodeUpdated = true) }
    }

    /**
     * Method handles sent message to chat click from UI
     *
     * returns if user is not online
     * updates [ContactInfoState.isStorageOverQuota] if storage state is [StorageState.PayWall]
     * creates chatroom exists else returns existing chat room
     * updates [ContactInfoState.shouldNavigateToChat] to true
     */
    fun sendMessageToChat() = viewModelScope.launch {
        if (!isOnline()) return@launch
        if (getStorageState() === StorageState.PayWall) {
            _state.update { it.copy(isStorageOverQuota = true) }
        } else {
            startConversation()
        }
    }

    private suspend fun startConversation() {
        userHandle?.let {
            runCatching {
                startConversationUseCase(isGroup = false, userHandles = listOf(it))
            }.onSuccess {
                val chatRoom = getChatRoom(it)
                chatRoom?.let {
                    _state.update { state ->
                        state.copy(chatRoom = chatRoom, shouldNavigateToChat = true)
                    }
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(snackBarMessage = R.string.create_chat_error)
                }
            }
        }
    }

    /**
     * Method handles chat notification click
     *
     * Creates chatroom if chatroom is not existing
     * updates [ContactInfoState.isChatNotificationChange] to true
     */
    fun chatNotificationsClicked() = viewModelScope.launch {
        if (chatId == null || chatId == INVALID_CHAT_HANDLE) {
            createChatRoom()
        }
        _state.update { it.copy(isChatNotificationChange = true) }
    }

    private suspend fun createChatRoom() {
        userHandle?.let {
            runCatching {
                createChatRoomUseCase(isGroup = false, userHandles = listOf(it))
            }.onSuccess {
                val chatRoom = getChatRoom(it)
                _state.update { state ->
                    state.copy(chatRoom = chatRoom)
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(snackBarMessage = R.string.create_chat_error)
                }
            }
        }
    }

    /**
     * Init share key
     *
     * @param node
     */
    suspend fun initShareKey(node: MegaNode) = runCatching {
        createShareKey(node)
    }.onFailure {
        Timber.e(it)
    }

    /**
     * Check copy name collision
     *
     * Verifies duplicate name is available in target folder
     * @param handles results from copy result launcher
     * @param context context of parent activity
     */
    @SuppressLint("CheckResult")
    fun checkCopyNameCollision(handles: Pair<LongArray, Long>?, context: Context) {
        if (!isOnline()) {
            _state.update { it.copy(snackBarMessage = R.string.error_server_connection_problem) }
            return
        }
        val copyHandles = handles?.first
        val toHandle = handles?.second
        if (copyHandles == null || toHandle == null) return
        _state.update { it.copy(isCopyInProgress = true) }
        checkNameCollisionUseCase.checkHandleList(
            copyHandles,
            toHandle,
            NameCollisionType.COPY,
            context,
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { (collisions, handlesWithoutCollision): Pair<ArrayList<NameCollision>, LongArray>, throwable: Throwable? ->
                if (throwable == null) {
                    if (collisions.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                isCopyInProgress = false,
                                nameCollisions = collisions,
                            )
                        }
                    }
                    if (handlesWithoutCollision.isNotEmpty()) {
                        copyNodes(handlesWithoutCollision, toHandle)
                    }
                }
            }
    }

    @SuppressLint("CheckResult")
    private fun copyNodes(handlesWithoutCollision: LongArray, toHandle: Long) {
        copyNodeUseCase.copy(handlesWithoutCollision, toHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { copyResult: CopyRequestResult, copyThrowable: Throwable? ->
                _state.update { it.copy(isCopyInProgress = false, isTransferComplete = true) }
                if (copyThrowable == null) {
                    _state.update {
                        it.copy(snackBarMessageString = copyRequestMessageMapper(copyResult))
                    }
                } else {
                    _state.update { it.copy(copyError = copyThrowable) }
                }
            }
    }

    companion object {
        private const val INVALID_CHAT_HANDLE = -1L
        private const val INVALID_NODE_HANDLE = -1L
    }
}